package heatmap

enum CellType:
  case Sink
  case Source(floorTemp: Double)
  case Air
  case Stair                      // open stairwell; conducts laterally like air, connects floors vertically
  case Solid(conductivity: Double)

case class Cell(cellType: CellType, temp: Double):
  def conductivity: Double = cellType match
    case CellType.Sink        => 1.0
    case CellType.Source(_)   => 1.0
    case CellType.Air         => Grid.AIR_CONDUCTIVITY
    case CellType.Stair       => Grid.AIR_CONDUCTIVITY
    case CellType.Solid(c)    => c

  def withTemp(t: Double): Cell = cellType match
    case CellType.Sink          => this
    case CellType.Source(floor) => copy(temp = math.max(t, floor))
    case CellType.Air           => copy(temp = t)
    case CellType.Stair         => copy(temp = t)
    case CellType.Solid(_)      => copy(temp = t)
