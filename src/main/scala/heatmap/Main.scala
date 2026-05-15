package heatmap

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Main:
  val SIMULATION_SECONDS = 3600

  def main(args: Array[String]): Unit =
    val gridPath = if args.nonEmpty then args(0) else "grid.txt"
    val (initialGrid, minTemp, maxTemp) = GridLoader.load(gridPath)

    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val outDir = File(s"output/$timestamp")
    outDir.mkdirs()

    def outFile(n: Int) = File(outDir, s"$n.png")

    Renderer.render(initialGrid, outFile(0), minTemp, maxTemp)
    println(s"Frame 0 written to ${outDir.getPath}")

    var grid = initialGrid
    for second <- 1 to SIMULATION_SECONDS do
      grid = Simulator.runSecond(grid)
      Renderer.render(grid, outFile(second), minTemp, maxTemp)
      if second % 60 == 0 then println(s"Simulated $second seconds (frame $second)")

    println(s"Done. ${SIMULATION_SECONDS + 1} frames written to ${outDir.getPath}")
