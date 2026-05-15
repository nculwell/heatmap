This is an experimental project to simulate heat flow through a building.

The basic idea is that there is a grid that represents a building, and each square is either a heat source, a heat sink, or neither one. In each simulation step, heat flows from warmer squares to cooler squares according to the conductivity of the receiving square. Each simulation step is 0.1 second.

The heat sinks are outdoors. They are all set to a certain temperature, and they don't change temperature when heat flows into them.

The heat sources are indoors, they are heaters. They always maintain a certain temperature floor. If heat flows into them then they can get hotter, but they won't go below their floor.

The program will write an image of the grid once per second, starting before any simulation steps run, and after the final step of each second. To start with, it will run for 1 hour of simulation time.
