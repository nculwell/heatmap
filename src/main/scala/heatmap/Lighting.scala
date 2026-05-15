package heatmap

object Lighting:

  // Returns a grid of light levels in [0, 1], normalised so the brightest cell = 1.
  // Transparent cells (air, heaters, stairs, light sources) pass light freely.
  // Opaque cells (solid walls, sinks) block light from passing through but can
  // themselves be lit if they have line-of-sight to a source.
  def computeLevels(grid: Grid): Vector[Vector[Double]] =
    val sources = for
      r <- 0 until grid.height
      c <- 0 until grid.width
      if grid(r, c).cellType == CellType.Light
    yield (r, c)

    if sources.isEmpty then return Vector.fill(grid.height, grid.width)(0.0)

    val raw = Vector.tabulate(grid.height, grid.width): (r, c) =>
      sources.map: (sr, sc) =>
        if hasLineOfSight(grid, r, c, sr, sc) then
          val dr = (r - sr).toDouble
          val dc = (c - sc).toDouble
          val dist = math.sqrt(dr * dr + dc * dc)
          1.0 / (1.0 + dist * 0.15)
        else 0.0
      .sum

    val maxVal = raw.flatten.maxOption.getOrElse(0.0)
    if maxVal == 0.0 then raw else raw.map(_.map(_ / maxVal))

  // Returns true if no opaque cell lies strictly between (r0,c0) and (r1,c1).
  // Uses linear interpolation to sample each intermediate cell along the line.
  private def hasLineOfSight(grid: Grid, r0: Int, c0: Int, r1: Int, c1: Int): Boolean =
    val steps = math.max(math.abs(r1 - r0), math.abs(c1 - c0))
    if steps == 0 then return true
    (1 until steps).forall: i =>
      val r = math.round(r0 + (r1 - r0).toDouble * i / steps).toInt
      val c = math.round(c0 + (c1 - c0).toDouble * i / steps).toInt
      !isOpaque(grid(r, c).cellType)

  // Opaque: blocks light passing through. Solid walls and exterior sinks are opaque.
  // Air, heaters, stairs, and light sources are transparent.
  private def isOpaque(ct: CellType): Boolean = ct match
    case CellType.Solid(_) | CellType.Sink => true
    case _                                 => false
