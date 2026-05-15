package heatmap

object Simulator:
  val DT               = 0.1
  val STEPS_PER_SECOND = 10

  private val CONV_BUOYANCY      = 0.3    // acceleration per unit temperature gradient (cells/s^2 per F/cell)
  private val CONV_DAMPING       = 0.9    // fraction of velocity retained each step (viscosity)
  private val MAX_VELOCITY       = 3.0    // cells/s, CFL safety cap
  private val FLOOR_CONDUCTIVITY = 0.15   // heat through floor/ceiling (wood)
  private val STAIR_CONDUCTIVITY = 0.70   // heat through stairwell (open air shaft)
  private val ROOF_CONDUCTIVITY  = 0.08   // heat through roof to outdoor (insulated)

  private val dirs = List((-1, 0), (1, 0), (0, -1), (0, 1))

  def step(building: Building): Building =
    building
      |> conductionStep
      |> interFloorStep
      |> roofStep
      |> convectionStep

  def runSecond(building: Building): Building =
    (0 until STEPS_PER_SECOND).foldLeft(building)((b, _) => step(b))

  // ---- per-floor conduction ----

  private def conductionStep(building: Building): Building =
    building.copy(
      floor1 = conductionGrid(building.floor1),
      floor2 = conductionGrid(building.floor2),
    )

  private def conductionGrid(grid: Grid): Grid =
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

  // ---- inter-floor heat transfer ----

  private def interFloorStep(building: Building): Building =
    val f1 = building.floor1
    val f2 = building.floor2
    val newCells1 = Vector.tabulate(f1.height, f1.width): (r, c) =>
      val c1   = f1(r, c)
      val c2   = f2(r, c)
      val cond = if c1.cellType == CellType.Stair then STAIR_CONDUCTIVITY else FLOOR_CONDUCTIVITY
      c1.withTemp(c1.temp + cond * (c2.temp - c1.temp) * DT)
    val newCells2 = Vector.tabulate(f2.height, f2.width): (r, c) =>
      val c1   = f1(r, c)
      val c2   = f2(r, c)
      val cond = if c2.cellType == CellType.Stair then STAIR_CONDUCTIVITY else FLOOR_CONDUCTIVITY
      c2.withTemp(c2.temp + cond * (c1.temp - c2.temp) * DT)
    building.copy(floor1 = Grid(newCells1), floor2 = Grid(newCells2))

  // ---- roof heat loss ----

  private def roofStep(building: Building): Building =
    val outdoor = building.sinkTemp
    val newCells2 = Vector.tabulate(building.floor2.height, building.floor2.width): (r, c) =>
      val cell = building.floor2(r, c)
      cell.withTemp(cell.temp + ROOF_CONDUCTIVITY * (outdoor - cell.temp) * DT)
    building.copy(floor2 = Grid(newCells2))

  // ---- per-floor convection with stored velocity ----
  // Velocity is driven by -∇T (pressure-driven outward flow from hot regions)
  // and decays each step via viscous damping.

  private def convectionStep(building: Building): Building =
    val (g1, vr1, vc1) = convectionFloor(building.floor1, building.vr1, building.vc1)
    val (g2, vr2, vc2) = convectionFloor(building.floor2, building.vr2, building.vc2)
    building.copy(floor1 = g1, floor2 = g2, vr1 = vr1, vc1 = vc1, vr2 = vr2, vc2 = vc2)

  private def convectionFloor(
    grid: Grid,
    vr: Vector[Vector[Double]],
    vc: Vector[Vector[Double]],
  ): (Grid, Vector[Vector[Double]], Vector[Vector[Double]]) =
    val vrCur = if vr.isEmpty then Vector.fill(grid.height, grid.width)(0.0) else vr
    val vcCur = if vc.isEmpty then Vector.fill(grid.height, grid.width)(0.0) else vc

    // Update velocity: buoyancy accelerates away from hot, viscosity damps it.
    val newVr = Vector.tabulate(grid.height, grid.width): (r, c) =>
      if !isFluid(grid(r, c).cellType) then 0.0
      else clamp(CONV_DAMPING * vrCur(r)(c) - CONV_BUOYANCY * centralDiff(grid, r, c, 1, 0) * DT)

    val newVc = Vector.tabulate(grid.height, grid.width): (r, c) =>
      if !isFluid(grid(r, c).cellType) then 0.0
      else clamp(CONV_DAMPING * vcCur(r)(c) - CONV_BUOYANCY * centralDiff(grid, r, c, 0, 1) * DT)

    // Advect temperature along the updated velocity field.
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      if !isFluid(cell.cellType) then cell
      else
        val delta = upwindAdv(grid, r, c, 1, 0, newVr(r)(c)) +
                    upwindAdv(grid, r, c, 0, 1, newVc(r)(c))
        cell.withTemp(cell.temp + delta * DT)

    (Grid(newCells), newVr, newVc)

  private def centralDiff(grid: Grid, r: Int, c: Int, dr: Int, dc: Int): Double =
    val T   = grid(r, c).temp
    val fwd = if inBounds(grid, r + dr, c + dc) then grid(r + dr, c + dc).temp else T
    val bwd = if inBounds(grid, r - dr, c - dc) then grid(r - dr, c - dc).temp else T
    (fwd - bwd) / 2.0

  private def upwindAdv(grid: Grid, r: Int, c: Int, dr: Int, dc: Int, v: Double): Double =
    val T = grid(r, c).temp
    if v > 0 then      -v * (T - fluidTemp(grid, r - dr, c - dc, T))
    else if v < 0 then -v * (fluidTemp(grid, r + dr, c + dc, T) - T)
    else 0.0

  private def fluidTemp(grid: Grid, nr: Int, nc: Int, fallback: Double): Double =
    if inBounds(grid, nr, nc) && isFluid(grid(nr, nc).cellType)
    then grid(nr, nc).temp
    else fallback

  private def isFluid(ct: CellType): Boolean =
    ct == CellType.Air || ct == CellType.Stair || ct == CellType.Light

  private def clamp(v: Double): Double = v.max(-MAX_VELOCITY).min(MAX_VELOCITY)

  private def inBounds(grid: Grid, r: Int, c: Int): Boolean =
    r >= 0 && r < grid.height && c >= 0 && c < grid.width

  extension [A](a: A)
    private inline def |>(f: A => A): A = f(a)
