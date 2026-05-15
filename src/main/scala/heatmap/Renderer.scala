package heatmap

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object Renderer:
  private val CELL_PX = 12  // pixels per grid cell

  def render(grid: Grid, outFile: File, minTemp: Double, maxTemp: Double): Unit =
    val w = grid.width  * CELL_PX
    val h = grid.height * CELL_PX
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    for
      row <- 0 until grid.height
      col <- 0 until grid.width
    do
      val t = grid(row, col).temp
      val rgb = tempToRgb(t, minTemp, maxTemp)
      for
        py <- row * CELL_PX until (row + 1) * CELL_PX
        px <- col * CELL_PX until (col + 1) * CELL_PX
      do img.setRGB(px, py, rgb)
    ImageIO.write(img, "png", outFile)

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
    val r = (r0 + frac * (r1 - r0)).toInt
    val g = (g0 + frac * (g1 - g0)).toInt
    val b = (b0 + frac * (b1 - b0)).toInt
    (r << 16) | (g << 8) | b

  private def findSegment(stops: Array[(Double, (Int, Int, Int))], t: Double): (Int, Int) =
    val i = stops.indexWhere((stop, _) => stop > t) match
      case -1 => stops.length - 2
      case n  => (n - 1).max(0)
    (i, i + 1)
