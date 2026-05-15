package heatmap

enum CellType:
  case Sink
  case Source(floorTemp: Double)
  case Neutral(conductivity: Double)

case class Cell(cellType: CellType, temp: Double):
  def conductivity: Double = cellType match
    case CellType.Sink          => 0.0
    case CellType.Source(_)     => 1.0
    case CellType.Neutral(c)    => c

  def withTemp(t: Double): Cell = cellType match
    case CellType.Sink            => this
    case CellType.Source(floor)   => copy(temp = math.max(t, floor))
    case CellType.Neutral(_)      => copy(temp = t)
