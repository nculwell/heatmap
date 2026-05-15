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
| `L` | Light source -- emits light; transparent and behaves like air for heat |
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

## Output

Each run creates a timestamped output directory (e.g., `output/20260515_143022`). Two types
of images are written:

- `light.png` -- a single static light map written once at the start
- `0000.png`, `0001.png`, etc. -- one heat map per simulated second, two floors side by side

The simulation runs until convergence or 1 hour of simulated time, whichever comes first.

### Temperature Color Key

Each cell is drawn as a small square. The center dot shows temperature; the border shows cell
type (see below). The temperature color scale runs from `heatSinkTemp` (coldest) to
`heatSourceTemp` (hottest):

| Fraction | Color |
|----------|-------|
| 0% (coldest) | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/000000'/> |
| 20% | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/0000C8'/> |
| 40% | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/00C8C8'/> |
| 60% | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/00C800'/> |
| 75% | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/C8C800'/> |
| 90% | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/C80000'/> |
| 100% (hottest) | <img valign='middle' alt='blue' src='https://readme-swatches.vercel.app/FFFFFF'/> |

### Cell Border Colors (heat map)

| Color | Cell type |
|-------|-----------|
| `#0078DC` | Heat sink (Z) |
| `#DC5000` | Heat source (H) |
| `#C8AA00` | Stairway (S) |
| `#FFFF96` | Light source (L) |
| `#373737` | Open air (.) |
| `#464646` to `#E6E6E6` | Solid material (1-9, darker = more insulating, lighter = more conductive) |

### Light Map

The light map (`light.png`) uses the same cell layout as the heat map. The center dot shows
light level from black (no light) to white (maximum). Cell borders use the same colors as the
heat map, except all solid walls share a uniform `#969696` border (insulation is irrelevant for
lighting), and heat sinks show as plain air (dark gray border).

Light propagates by raycasting from each `L` cell. Solid walls and sinks block light but their
facing surfaces can be lit. Light bleeds slightly around corners and passes through stairways
between floors.

## Simulation Parameters

- Cell size: 0.25 m (a 40x40 grid represents a 10m x 10m floor plan)
- Time step: 0.1 seconds (10 steps per simulated second)
- Images written: once per simulated second; two floors shown side by side
- Neighbors: 4-directional (up, down, left, right)
- Temperature units: Fahrenheit
- Image color scale: fixed (sink temp = cold, source temp = hot)
- Stops at convergence (max temperature change < 0.1 F/s) or after 1 hour of simulated time
- Heat transfer: conduction, convection (stored velocity field), inter-floor conduction, roof loss
- See [convection.md](convection.md) for full details of the heat transfer model
