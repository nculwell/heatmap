package heatmap

object Simulator:
  val DT               = 0.1  // seconds per step
  val STEPS_PER_SECOND = 10

  private val CONV_ALPHA    = 0.3  // buoyancy force: acceleration per unit temperature gradient
  private val CONV_VISCOSITY = 0.2  // fraction of velocity lost to viscosity each step
  private val MAX_VELOCITY  = 5.0  // cells per second, caps velocity for numerical stability

  private val dirs = List((-1, 0), (1, 0), (0, -1), (0, 1))

  def step(state: SimState): SimState =
    convectionStep(conductionStep(state))

  def runSecond(state: SimState): SimState =
    (0 until STEPS_PER_SECOND).foldLeft(state)((s, _) => step(s))

  // ---- conduction ----

  private def conductionStep(state: SimState): SimState =
    val grid = state.grid
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
    state.copy(grid = Grid(newCells))

  // ---- convection ----

  private def convectionStep(state: SimState): SimState =
    val grid = state.grid

    // Step 1: update velocity with buoyancy force and viscosity damping.
    // Force direction: -∇T (air accelerates from hot toward cold, like pressure-driven flow).
    val newVr = Vector.tabulate(grid.height, grid.width): (r, c) =>
      if grid(r, c).cellType != CellType.Air then 0.0
      else
        val grad = centralDiff(grid, r, c, dr = 1, dc = 0)
        clamp((1.0 - CONV_VISCOSITY) * state.vr(r)(c) - CONV_ALPHA * grad * DT)

    val newVc = Vector.tabulate(grid.height, grid.width): (r, c) =>
      if grid(r, c).cellType != CellType.Air then 0.0
      else
        val grad = centralDiff(grid, r, c, dr = 0, dc = 1)
        clamp((1.0 - CONV_VISCOSITY) * state.vc(r)(c) - CONV_ALPHA * grad * DT)

    // Step 2: advect temperature along the updated velocity field (upwind scheme).
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      if cell.cellType != CellType.Air then cell
      else
        val vr = newVr(r)(c)
        val vc = newVc(r)(c)
        val delta = upwindAdv(grid, r, c, dr = 1, dc = 0, v = vr) +
                    upwindAdv(grid, r, c, dr = 0, dc = 1, v = vc)
        cell.withTemp(cell.temp + delta * DT)

    SimState(Grid(newCells), newVr, newVc)

  // Central difference of temperature along axis (dr, dc).
  private def centralDiff(grid: Grid, r: Int, c: Int, dr: Int, dc: Int): Double =
    val T   = grid(r, c).temp
    val fwd = if inBounds(grid, r + dr, c + dc) then grid(r + dr, c + dc).temp else T
    val bwd = if inBounds(grid, r - dr, c - dc) then grid(r - dr, c - dc).temp else T
    (fwd - bwd) / 2.0

  // Upwind advection contribution (returns dT/dt) along axis (dr, dc) with velocity v.
  // Only steps into adjacent air cells; treats non-air as a no-flow boundary.
  private def upwindAdv(grid: Grid, r: Int, c: Int, dr: Int, dc: Int, v: Double): Double =
    val T = grid(r, c).temp
    if v > 0 then
      // flow in + direction: temperature comes from the − side
      val bwdT = airTemp(grid, r - dr, c - dc, T)
      -v * (T - bwdT)
    else if v < 0 then
      // flow in − direction: temperature comes from the + side
      val fwdT = airTemp(grid, r + dr, c + dc, T)
      -v * (fwdT - T)
    else 0.0

  // Temperature of an air cell at (nr, nc), or the current cell's temp if out of bounds / not air.
  private def airTemp(grid: Grid, nr: Int, nc: Int, fallback: Double): Double =
    if inBounds(grid, nr, nc) && grid(nr, nc).cellType == CellType.Air
    then grid(nr, nc).temp
    else fallback

  private def clamp(v: Double): Double = v.max(-MAX_VELOCITY).min(MAX_VELOCITY)

  private def inBounds(grid: Grid, r: Int, c: Int): Boolean =
    r >= 0 && r < grid.height && c >= 0 && c < grid.width
