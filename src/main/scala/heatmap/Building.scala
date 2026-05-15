package heatmap

case class Building(
  floor1: Grid,
  floor2: Grid,
  sinkTemp: Double,
  vr1: Vector[Vector[Double]] = Vector.empty,
  vc1: Vector[Vector[Double]] = Vector.empty,
  vr2: Vector[Vector[Double]] = Vector.empty,
  vc2: Vector[Vector[Double]] = Vector.empty,
):
  def maxDelta(other: Building): Double =
    math.max(floor1.maxDelta(other.floor1), floor2.maxDelta(other.floor2))

object Building:
  def fromFloor1(floor1: Grid, sinkTemp: Double): Building =
    val floor2Cells = floor1.cells.map: row =>
      row.map: cell =>
        cell.cellType match
          case CellType.Source(_) => Cell(CellType.Air, sinkTemp)
          case _                  => cell
    Building(floor1, Grid(floor2Cells), sinkTemp)
