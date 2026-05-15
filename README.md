# Heatmap

A simulation of heat flow through a building, written in Scala.

## Grid Data File Format

The grid layout is read from a text file with the following structure:

```
heatSourceTemp=<value>
heatSinkTemp=<value>
width=<value>
height=<value>
<grid rows>
```

### Cell Types

| Character | Meaning |
|-----------|---------|
| `O` | Heat sink (outdoors) — fixed temperature, never changes |
| `H` | Heat source (heater) — maintains a temperature floor |
| `.` | Open air — neutral cell with low conductivity |
| `1`–`9` | Solid material — digit indicates conductivity (1=insulator, 9=conductor) |

### Material Conductivity Reference

Digits 1–9 map linearly to a conductivity range of ~0.02 (near-zero) to ~0.95 (close to 1.0).
Open air (`.`) is treated as a fixed internal conductivity equivalent to digit 2 (~0.23).

| Material | Digit | Notes |
|----------|-------|-------|
| Fiberglass insulation | 1 | Near-perfect insulator |
| Wood panel wall | 2–3 | Modest insulator (~0.15 W/m·K) |
| Drywall with studs | 3–4 | Studs create thermal bridges |
| Brick | 5 | Middle of the spectrum (~0.7 W/m·K) |
| Stone wall | 6–7 | Dense stone or concrete (~1.5–2 W/m·K) |
| Poured concrete | 7–8 | Very good conductor for building materials |

Convection modeling is planned for a future iteration.

## Output

Each run creates a timestamped output directory (e.g., `output/20260515_143022`). Images are written as `0.png`, `1.png`, etc., one per simulated second. The simulation runs for 1 hour (3,600 seconds), producing 3,601 images total (including the initial frame).

## Simulation Parameters

- Time step: 0.1 seconds
- Images written: once per simulated second
- Neighbors: 4-directional (up, down, left, right)
- Temperature units: Fahrenheit
- Image color scale: fixed (sink temp = cold, source temp = hot)
