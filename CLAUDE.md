This is an experimental project to simulate heat flow through a building, written in Scala.

Each grid cell is 0.25m across. A 40x40 grid represents a 10m x 10m floor plan.

## Heat Transfer

Each simulation step is 0.1 seconds. Four mechanisms run every step:

1. **Conduction** -- heat flows between neighboring cells (4-directional) at a rate determined
   by the receiving cell's conductivity. Sinks stay fixed; sources enforce a temperature floor.

2. **Convection** -- air and stairway cells have a stored velocity field that persists between
   steps. Velocity is driven by -grad(T) (outward from hot regions, pressure-driven) with viscous
   damping. Temperature is advected along the velocity using an upwind scheme.

3. **Inter-floor conduction** -- every cell pair (floor 1 below, floor 2 above) exchanges heat
   each step. Stairway cells use a high conductivity (0.70); all other pairs use floor
   conductivity (0.15, like wood).

4. **Roof heat loss** -- every floor-2 cell loses heat to sinkTemp through the roof (0.08).

See convection.md for full details and parameter values.

## Grid Data File Format

```
heatSourceTemp=<value>
heatSinkTemp=<value>
[floor1]
<grid rows>
[floor2]
<grid rows>
```

Width and height are inferred from the grid data. The loader validates that all rows have the
same width and that both floors have matching dimensions.

Each cell is one ASCII character:

- `Z` -- heat sink (outdoors); fixed temperature, never changes
- `H` -- heat source (heater); maintains a temperature floor, will not drop below it
- `S` -- stairway; open shaft connecting floors, conducts laterally like air
- `.` -- open air; low conductivity
- `1`-`9` -- solid material; digit is conductivity (1=insulator, 9=conductor)

Floor 2 is defined explicitly in the grid file (not auto-generated from floor 1). It typically
has the same walls/sinks as floor 1 but no heaters. Stairway cells (S) should appear at the
same position on both floors.

## Output

Each run creates a timestamped output directory (e.g., `output/20260515_143022`). Images are
written as `0000.png`, `0001.png`, etc., one per simulated second. Each image shows floor 1 on
the left and floor 2 on the right, separated by a black gap.

Each cell is rendered as a 12x12 pixel square: black background, 1px border showing cell type,
and a 4x4 center dot showing temperature. See README.md for the color key.

The simulation stops at convergence (max temperature change < 0.1 F across all cells in one
simulated second) or after 1 hour of simulated time, whichever comes first.

## Running

```
sbt run                        # uses grid.txt
sbt "run path/to/grid.txt"     # uses a specific grid file
```

sbt is at `%LOCALAPPDATA%\Coursier\data\bin\sbt.bat` on this machine.

## Source Layout

```
src/main/scala/heatmap/
  Main.scala        -- entry point, simulation loop, output directory
  Building.scala    -- holds floor1, floor2 grids and stored velocity fields
  Grid.scala        -- 2D cell array, parsing, validation
  Cell.scala        -- CellType enum, Cell case class, conductivity/withTemp
  GridLoader.scala  -- parses grid file into Building
  Simulator.scala   -- all heat transfer logic (step, runSecond)
  Renderer.scala    -- temperature-to-color mapping, side-by-side PNG output
  SimState.scala    -- unused, can be deleted
```
