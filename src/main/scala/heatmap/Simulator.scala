package heatmap

object Simulator:
  val DT               = 0.1
  val STEPS_PER_SECOND = 10

  private val CONV_ALPHA   = 0.05  // velocity = CONV_ALPHA * |∇T|; kept small for CFL stability
  private val MAX_VELOCITY = 3.0   // cells/s, hard cap for safety

  private val dirs = List((-1, 0), (1, 0), (0, -1), (0, 1))

  def step(grid: Grid): Grid = convectionStep(conductionStep(grid))

  def runSecond(grid: Grid): Grid =
    (0 until STEPS_PER_SECOND).foldLeft(grid)((g, _) => step(g))

  // ---- conduction ----

  private def conductionStep(grid: Grid): Grid =
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      val delta = dirs.map: (dr, dc) =>
        val nr = r + dr; val nc = c + dc
        if inBounds(grid, nr, nc) then
          val neighbor = grid(nr, nc)
          val diff = neighbor.temp - cell.temp
          if diff > 0 then cell.conductivity * diff * DT
          else           neighbor.conductivity * diff * DT
        else 0.0
      .sum
      cell.withTemp(cell.temp + delta)
    Grid(newCells)

  // ---- convection ----
  // Steady-state velocity: v = -CONV_ALPHA * ∇T (air flows from hot toward cold).
  // Temperature is then advected along v using an upwind scheme.

  private def convectionStep(grid: Grid): Grid =
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      if cell.cellType != CellType.Air then cell
      else
        val vr = clamp(-CONV_ALPHA * centralDiff(grid, r, c, dr = 1, dc = 0))
        val vc = clamp(-CONV_ALPHA * centralDiff(grid, r, c, dr = 0, dc = 1))
        val delta = upwindAdv(grid, r, c, dr = 1, dc = 0, v = vr) +
                    upwindAdv(grid, r, c, dr = 0, dc = 1, v = vc)
        cell.withTemp(cell.temp + delta * DT)
    Grid(newCells)

  // Central difference of temperature along axis (dr, dc).
  private def centralDiff(grid: Grid, r: Int, c: Int, dr: Int, dc: Int): Double =
    val T   = grid(r, c).temp
    val fwd = if inBounds(grid, r + dr, c + dc) then grid(r + dr, c + dc).temp else T
    val bwd = if inBounds(grid, r - dr, c - dc) then grid(r - dr, c - dc).temp else T
    (fwd - bwd) / 2.0

  // Upwind advection: returns dT/dt contribution along axis (dr, dc) with velocity v.
  // Non-air cells act as no-flow boundaries.
  private def upwindAdv(grid: Grid, r: Int, c: Int, dr: Int, dc: Int, v: Double): Double =
    val T = grid(r, c).temp
    if v > 0 then
      -v * (T - airTemp(grid, r - dr, c - dc, T))
    else if v < 0 then
      -v * (airTemp(grid, r + dr, c + dc, T) - T)
    else 0.0

  private def airTemp(grid: Grid, nr: Int, nc: Int, fallback: Double): Double =
    if inBounds(grid, nr, nc) && grid(nr, nc).cellType == CellType.Air
    then grid(nr, nc).temp
    else fallback

  private def clamp(v: Double): Double = v.max(-MAX_VELOCITY).min(MAX_VELOCITY)

  private def inBounds(grid: Grid, r: Int, c: Int): Boolean =
    r >= 0 && r < grid.height && c >= 0 && c < grid.width
