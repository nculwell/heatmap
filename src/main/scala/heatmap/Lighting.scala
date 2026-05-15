package heatmap

object Lighting:
  private val BLEED_FACTOR = 0.35  // fraction of a cell's light that bleeds to each neighbor per pass
  private val BLEED_PASSES = 3     // number of corner-bleed iterations
  private val STAIR_FACTOR = 0.7   // fraction of stair-cell light transmitted to the other floor

  private val dirs = List((-1, 0), (1, 0), (0, -1), (0, 1))

  // Compute normalised light levels [0,1] for both floors, including stair transfer and corner bleed.
  def computeAll(building: Building): (Vector[Vector[Double]], Vector[Vector[Double]]) =
    var levels1 = directLight(building.floor1)
    var levels2 = directLight(building.floor2)

    // Transfer light between floors through stairway cells (before bleed so it can spread).
    for
      r <- 0 until building.floor1.height
      c <- 0 until building.floor1.width
      if building.floor1(r, c).cellType == CellType.Stair
    do
      val l1 = levels1(r)(c)
      val l2 = levels2(r)(c)
      levels1 = levels1.updated(r, levels1(r).updated(c, math.max(l1, l2 * STAIR_FACTOR)))
      levels2 = levels2.updated(r, levels2(r).updated(c, math.max(l2, l1 * STAIR_FACTOR)))

    // Spread light around corners.
    for _ <- 0 until BLEED_PASSES do
      levels1 = bleedPass(building.floor1, levels1)
      levels2 = bleedPass(building.floor2, levels2)

    (normalize(levels1), normalize(levels2))

  // Raw (un-normalised) direct light from raycasting only.
  private def directLight(grid: Grid): Vector[Vector[Double]] =
    val sources = for
      r <- 0 until grid.height
      c <- 0 until grid.width
      if grid(r, c).cellType == CellType.Light
    yield (r, c)

    if sources.isEmpty then return Vector.fill(grid.height, grid.width)(0.0)

    Vector.tabulate(grid.height, grid.width): (r, c) =>
      sources.map: (sr, sc) =>
        if hasLineOfSight(grid, r, c, sr, sc) then
          val dr = (r - sr).toDouble
          val dc = (c - sc).toDouble
          1.0 / (1.0 + math.sqrt(dr * dr + dc * dc) * 0.15)
        else 0.0
      .sum

  // One bleed pass: each non-opaque cell picks up a fraction of its brightest non-opaque neighbor.
  private def bleedPass(grid: Grid, levels: Vector[Vector[Double]]): Vector[Vector[Double]] =
    Vector.tabulate(grid.height, grid.width): (r, c) =>
      if isOpaque(grid(r, c).cellType) then levels(r)(c)
      else
        val maxNeighbor = dirs.flatMap: (dr, dc) =>
          val nr = r + dr; val nc = c + dc
          if nr >= 0 && nr < grid.height && nc >= 0 && nc < grid.width &&
             !isOpaque(grid(nr, nc).cellType)
          then Some(levels(nr)(nc))
          else None
        .maxOption.getOrElse(0.0)
        math.max(levels(r)(c), maxNeighbor * BLEED_FACTOR)



  private def normalize(levels: Vector[Vector[Double]]): Vector[Vector[Double]] =
    val maxVal = levels.flatten.maxOption.getOrElse(0.0)
    if maxVal == 0.0 then levels else levels.map(_.map(_ / maxVal))

  // True if no opaque cell lies strictly between (r0,c0) and (r1,c1).
  private def hasLineOfSight(grid: Grid, r0: Int, c0: Int, r1: Int, c1: Int): Boolean =
    val steps = math.max(math.abs(r1 - r0), math.abs(c1 - c0))
    if steps == 0 then return true
    (1 until steps).forall: i =>
      val r = math.round(r0 + (r1 - r0).toDouble * i / steps).toInt
      val c = math.round(c0 + (c1 - c0).toDouble * i / steps).toInt
      !isOpaque(grid(r, c).cellType)

  // Opaque: blocks light passing through. Transparent: air, heaters, stairs, light sources.
  private def isOpaque(ct: CellType): Boolean = ct match
    case CellType.Solid(_) | CellType.Sink => true
    case _                                 => false
