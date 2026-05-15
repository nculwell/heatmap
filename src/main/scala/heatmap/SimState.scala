package heatmap

case class SimState(
  grid: Grid,
  vr:   Vector[Vector[Double]],  // row-axis velocity (positive = downward)
  vc:   Vector[Vector[Double]],  // col-axis velocity (positive = rightward)
):
  def maxDelta(other: SimState): Double = grid.maxDelta(other.grid)

object SimState:
  def initial(grid: Grid): SimState =
    val zero = Vector.fill(grid.height, grid.width)(0.0)
    SimState(grid, zero, zero)
