package heatmap

case class Grid(cells: Vector[Vector[Cell]]):
  val height: Int = cells.length
  val width: Int  = cells.headOption.map(_.length).getOrElse(0)

  def apply(row: Int, col: Int): Cell = cells(row)(col)

  def updated(row: Int, col: Int, cell: Cell): Grid =
    Grid(cells.updated(row, cells(row).updated(col, cell)))

  def maxDelta(other: Grid): Double =
    var max = 0.0
    for r <- cells.indices; c <- cells(r).indices do
      val d = math.abs(cells(r)(c).temp - other.cells(r)(c).temp)
      if d > max then max = d
    max

object Grid:
  val AIR_CONDUCTIVITY: Double = digitToConductivity(2)

  def digitToConductivity(d: Int): Double =
    val lo = 0.02
    val hi = 0.95
    lo + (d - 1) / 8.0 * (hi - lo)

  def fromLines(meta: Map[String, String], rows: Seq[String]): Grid =
    val sourceTemp = meta("heatSourceTemp").toDouble
    val sinkTemp   = meta("heatSinkTemp").toDouble
    val cells = rows.toVector.map: row =>
      row.toVector.map: ch =>
        val cellType = ch match
          case 'Z'            => CellType.Sink
          case 'H'            => CellType.Source(sourceTemp)
          case '.'            => CellType.Air
          case 'S'            => CellType.Stair
          case d if d.isDigit => CellType.Solid(digitToConductivity(d.asDigit))
          case other          => throw IllegalArgumentException(s"Unknown cell char: $other")
        val initTemp = ch match
          case 'Z' => sinkTemp
          case 'H' => sourceTemp
          case _   => sinkTemp  // start everything else at ambient
        Cell(cellType, initTemp)
    Grid(cells)
