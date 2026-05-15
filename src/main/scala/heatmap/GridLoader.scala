package heatmap

import scala.io.Source

object GridLoader:
  def load(path: String): (Building, Double, Double) =
    val lines = Source.fromFile(path).getLines().toSeq
    val (metaLines, gridLines) = lines.span(_.contains('='))
    val meta = metaLines.map: line =>
      val Array(k, v) = line.split('=')
      k.trim -> v.trim
    .toMap
    val sinkTemp   = meta("heatSinkTemp").toDouble
    val sourceTemp = meta("heatSourceTemp").toDouble
    val floor1     = Grid.fromLines(meta, gridLines.filter(_.nonEmpty))
    val building   = Building.fromFloor1(floor1, sinkTemp)
    (building, sinkTemp, sourceTemp)
