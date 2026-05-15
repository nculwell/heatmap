package heatmap

import scala.io.Source

object GridLoader:
  def load(path: String): (Grid, Double, Double) =
    val lines = Source.fromFile(path).getLines().toSeq
    val (metaLines, gridLines) = lines.span(_.contains('='))
    val meta = metaLines.map: line =>
      val Array(k, v) = line.split('=')
      k.trim -> v.trim
    .toMap
    val grid = Grid.fromLines(meta, gridLines.filter(_.nonEmpty))
    (grid, meta("heatSinkTemp").toDouble, meta("heatSourceTemp").toDouble)
