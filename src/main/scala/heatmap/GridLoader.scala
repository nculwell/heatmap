package heatmap

import scala.io.Source

object GridLoader:
  def load(path: String): (Building, Double, Double) =
    val lines = Source.fromFile(path).getLines().toSeq

    val metaLines  = lines.takeWhile(l => !l.startsWith("["))
    val sectionLines = lines.dropWhile(l => !l.startsWith("["))

    val meta = metaLines
      .filter(_.contains('='))
      .map: line =>
        val Array(k, v) = line.split('=')
        k.trim -> v.trim
      .toMap

    val sections = parseSections(sectionLines)
    val sinkTemp   = meta("heatSinkTemp").toDouble
    val sourceTemp = meta("heatSourceTemp").toDouble

    val floor1 = Grid.fromLines(meta, sections("floor1"))
    val floor2 = Grid.fromLines(meta, sections("floor2"))
    (Building(floor1, floor2, sinkTemp), sinkTemp, sourceTemp)

  private def parseSections(lines: Seq[String]): Map[String, Seq[String]] =
    lines.foldLeft(Map.empty[String, Seq[String]], Option.empty[String]):
      case ((acc, current), line) if line.startsWith("[") =>
        val name = line.stripPrefix("[").stripSuffix("]").trim
        (acc + (name -> Seq.empty), Some(name))
      case ((acc, Some(name)), line) if line.nonEmpty =>
        (acc + (name -> (acc(name) :+ line)), Some(name))
      case (state, _) => state
    ._1
