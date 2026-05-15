package heatmap

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object Renderer:
  private val CELL_PX  = 12
  private val DOT_SIZE = 4
  private val DOT_OFF  = (CELL_PX - DOT_SIZE) / 2  // offset to center the dot

  def render(grid: Grid, outFile: File, minTemp: Double, maxTemp: Double): Unit =
    val img = BufferedImage(grid.width * CELL_PX, grid.height * CELL_PX, BufferedImage.TYPE_INT_RGB)
    for
      row <- 0 until grid.height
      col <- 0 until grid.width
    do
      renderCell(img, grid(row, col), col * CELL_PX, row * CELL_PX, minTemp, maxTemp)
    ImageIO.write(img, "png", outFile)

  private def renderCell(img: BufferedImage, cell: Cell, x0: Int, y0: Int, minTemp: Double, maxTemp: Double): Unit =
    // black background
    for py <- y0 until y0 + CELL_PX; px <- x0 until x0 + CELL_PX do
      img.setRGB(px, py, 0)

    // 1px border in type color
    val border = typeColor(cell.cellType)
    for i <- 0 until CELL_PX do
      img.setRGB(x0 + i, y0,              border)
      img.setRGB(x0 + i, y0 + CELL_PX - 1, border)
      img.setRGB(x0,     y0 + i,          border)
      img.setRGB(x0 + CELL_PX - 1, y0 + i, border)

    // center dot in temperature color
    val dot = tempToRgb(cell.temp, minTemp, maxTemp)
    for dy <- 0 until DOT_SIZE; dx <- 0 until DOT_SIZE do
      img.setRGB(x0 + DOT_OFF + dx, y0 + DOT_OFF + dy, dot)

  private def typeColor(cellType: CellType): Int = cellType match
    case CellType.Sink      => rgb(0, 120, 220)       // blue
    case CellType.Source(_) => rgb(220, 80, 0)        // orange-red
    case CellType.Air       => rgb(55, 55, 55)        // dim gray
    case CellType.Solid(c)  =>
      // conductivity range 0.02–0.95 → gray 70–230 (darker=insulator, lighter=conductor)
      val t = (c - 0.02) / (0.95 - 0.02)
      val v = (70 + t * 160).toInt
      rgb(v, v, v)

  private def rgb(r: Int, g: Int, b: Int): Int = (r << 16) | (g << 8) | b

  private def tempToRgb(temp: Double, minTemp: Double, maxTemp: Double): Int =
    val t = ((temp - minTemp) / (maxTemp - minTemp)).max(0.0).min(1.0)
    // black -> blue -> cyan -> green -> yellow -> red -> white
    val stops = Array(
      (0.00, (0,   0,   0  )),
      (0.20, (0,   0,   200)),
      (0.40, (0,   200, 200)),
      (0.60, (0,   200, 0  )),
      (0.75, (200, 200, 0  )),
      (0.90, (200, 0,   0  )),
      (1.00, (255, 255, 255)),
    )
    val (i, j) = findSegment(stops, t)
    val (t0, (r0, g0, b0)) = stops(i)
    val (t1, (r1, g1, b1)) = stops(j)
    val frac = if t1 == t0 then 0.0 else (t - t0) / (t1 - t0)
    rgb(
      (r0 + frac * (r1 - r0)).toInt,
      (g0 + frac * (g1 - g0)).toInt,
      (b0 + frac * (b1 - b0)).toInt,
    )

  private def findSegment(stops: Array[(Double, (Int, Int, Int))], t: Double): (Int, Int) =
    val i = stops.indexWhere((stop, _) => stop > t) match
      case -1 => stops.length - 2
      case n  => (n - 1).max(0)
    (i, i + 1)
