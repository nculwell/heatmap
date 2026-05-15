# Heatmap

A simulation of heat flow through a building, written in Scala.

## Grid Data File Format

The grid layout is read from a text file with the following structure:

```
heatSourceTemp=<value>
heatSinkTemp=<value>
[floor1]
<grid rows>
[floor2]
<grid rows>
```

### Cell Types

| Character | Meaning |
|-----------|---------|
| `Z` | Heat sink (outdoors) -- fixed temperature, never changes |
| `H` | Heat source (heater) -- maintains a temperature floor |
| `S` | Stairway -- open shaft connecting floors vertically |
| `.` | Open air -- neutral cell with low conductivity |
| `1`-`9` | Solid material -- digit indicates conductivity (1=insulator, 9=conductor) |

### Material Conductivity Reference

Digits 1-9 map linearly to a conductivity range of ~0.02 (near-zero) to ~0.95 (close to 1.0).
Open air (`.`) is treated as a fixed internal conductivity equivalent to digit 2 (~0.23).

| Material              | Digit | Notes                                  |
|-----------------------|-------|----------------------------------------|
| Fiberglass insulation | 1     | Near-perfect insulator                 |
| Wood panel wall       | 2-3   | Modest insulator (~0.15 W/m K)         |
| Drywall with studs    | 3-4   | Studs create thermal bridges           |
| Brick                 | 5     | Middle of the spectrum (~0.7 W/m K)    |
| Stone wall            | 6-7   | Dense stone or concrete (~1.5-2 W/m K) |
| Poured concrete       | 7-8   | Very good conductor for building materials |

Convection is modeled. See [convection.md](convection.md) for details.

## Output

Each run creates a timestamped output directory (e.g., `output/20260515_143022`). Images are
written as `0000.png`, `0001.png`, etc., one per simulated second, with the two floors shown
side by side. The simulation runs until convergence or 1 hour of simulated time, whichever
comes first.

### Temperature Color Key

Each cell is drawn as a small square. The center dot shows temperature; the border shows cell
type (see below). The temperature color scale runs from `heatSinkTemp` (coldest) to
`heatSourceTemp` (hottest):

```
coldest |-------- temperature --------| hottest
  black   blue   cyan  green  yellow   red   white
   0%     20%    40%    60%    75%     90%   100%
```

### Cell Border Colors

| Border color | Cell type          |
|--------------|--------------------|
| Blue         | Heat sink (Z)      |
| Orange-red   | Heat source (H)    |
| Gold         | Stairway (S)       |
| Dark gray    | Open air (.)       |
| Light gray   | Solid material (1-9, darker = more insulating, lighter = more conductive) |

## Simulation Parameters

- Time step: 0.1 seconds
- Images written: once per simulated second
- Neighbors: 4-directional (up, down, left, right)
- Temperature units: Fahrenheit
- Image color scale: fixed (sink temp = cold, source temp = hot)
