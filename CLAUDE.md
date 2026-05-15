This is an experimental project to simulate heat flow through a building.

The basic idea is that there is a grid that represents a building, and each square is either a heat source, a heat sink, or neither one. In each simulation step, heat flows from warmer squares to cooler squares according to the conductivity of the receiving square. Each simulation step is 0.1 second.

The heat sinks are outdoors. They are all set to a certain temperature, and they don't change temperature when heat flows into them.

The heat sources are indoors, they are heaters. They always maintain a certain temperature floor. If heat flows into them then they can get hotter, but they won't go below their floor.

The program will write an image of the grid once per second, starting before any simulation steps run, and after the final step of each second. To start with, it will run for 1 hour of simulation time.

## Grid Data File Format

The grid layout is read from a text file. The file has the following structure:

```
heatSourceTemp=<value>
heatSinkTemp=<value>
width=<value>
height=<value>
<grid rows>
```

Each cell in the grid is represented by a single ASCII character:

- `O` — heat sink (outdoors); fixed temperature, never changes
- `H` — heat source (heater); has a temperature floor, will not drop below it
- `.` — open air; neutral cell with low conductivity
- `1`–`9` — solid material; neutral cell where the digit is the conductivity (1=insulator, 9=conductor)

The default grid size is 40x40, but dimensions are determined by the file.

## Output

Each run creates a new output directory named with the start timestamp (e.g., `output/20260515_143022`). Image files are written into that directory, numbered sequentially starting from 0 (e.g., `0.png`, `1.png`, ..., `3600.png`).
