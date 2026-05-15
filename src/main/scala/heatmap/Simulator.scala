package heatmap

object Simulator:
  val DT               = 0.1
  val STEPS_PER_SECOND = 10

  private val CONV_ALPHA         = 0.05   // convection: velocity per unit temperature gradient
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
  // Floor conduction applies to every cell pair; stairwell cells use a higher rate.
  // Both floors are updated from the pre-step values (explicit scheme).

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
  // Every floor-2 cell conducts heat through the roof to the outdoor sink temperature.

  private def roofStep(building: Building): Building =
    val outdoor = building.sinkTemp
    val newCells2 = Vector.tabulate(building.floor2.height, building.floor2.width): (r, c) =>
      val cell = building.floor2(r, c)
      cell.withTemp(cell.temp + ROOF_CONDUCTIVITY * (outdoor - cell.temp) * DT)
    building.copy(floor2 = Grid(newCells2))

  // ---- per-floor convection ----

  private def convectionStep(building: Building): Building =
    building.copy(
      floor1 = convectionGrid(building.floor1),
      floor2 = convectionGrid(building.floor2),
    )

  private def convectionGrid(grid: Grid): Grid =
    val newCells = Vector.tabulate(grid.height, grid.width): (r, c) =>
      val cell = grid(r, c)
      if !isFluid(cell.cellType) then cell
      else
        val vr = clamp(-CONV_ALPHA * centralDiff(grid, r, c, dr = 1, dc = 0))
        val vc = clamp(-CONV_ALPHA * centralDiff(grid, r, c, dr = 0, dc = 1))
        val delta = upwindAdv(grid, r, c, dr = 1, dc = 0, v = vr) +
                    upwindAdv(grid, r, c, dr = 0, dc = 1, v = vc)
        cell.withTemp(cell.temp + delta * DT)
    Grid(newCells)

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
    ct == CellType.Air || ct == CellType.Stair

  private def clamp(v: Double): Double = v.max(-MAX_VELOCITY).min(MAX_VELOCITY)

  private def inBounds(grid: Grid, r: Int, c: Int): Boolean =
    r >= 0 && r < grid.height && c >= 0 && c < grid.width

  extension [A](a: A)
    private inline def |>(f: A => A): A = f(a)
