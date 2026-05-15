package heatmap

object Simulator:
  val DT = 0.1  // seconds per step
  val STEPS_PER_SECOND = 10

  private val neighbors = List((-1, 0), (1, 0), (0, -1), (0, 1))

  def step(grid: Grid): Grid =
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      val delta = neighbors.map: (dr, dc) =>
        val nr = r + dr
        val nc = c + dc
        if nr >= 0 && nr < grid.height && nc >= 0 && nc < grid.width then
          val neighbor = grid(nr, nc)
          val diff = neighbor.temp - cell.temp
          if diff > 0 then
            cell.conductivity * diff * DT       // heat flows in: receiving cell's conductivity
          else
            neighbor.conductivity * diff * DT   // heat flows out: receiving neighbor's conductivity
        else 0.0
      .sum
      cell.withTemp(cell.temp + delta)
    Grid(newCells)

  def runSecond(grid: Grid): Grid =
    (0 until STEPS_PER_SECOND).foldLeft(grid)((g, _) => step(g))
