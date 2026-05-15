package heatmap

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Main:
  val SIMULATION_SECONDS = 3600

  def main(args: Array[String]): Unit =
    val gridPath = if args.nonEmpty then args(0) else "grid.txt"
    val (building, minTemp, maxTemp) = GridLoader.load(gridPath)

    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val outDir = File(s"output/$timestamp")
    outDir.mkdirs()

    def outFile(n: Int) = File(outDir, f"$n%04d.png")

    Renderer.renderLighting(building, File(outDir, "light.png"))
    println(s"Light map written to ${outDir.getPath}/light.png")

    Renderer.render(building, outFile(0), minTemp, maxTemp)
    println(s"Frame 0 written to ${outDir.getPath}")

    val convergenceThreshold = 0.1

    var state = building
    var second = 1
    var converged = false
    while second <= SIMULATION_SECONDS && !converged do
      val next = Simulator.runSecond(state)
      Renderer.render(next, outFile(second), minTemp, maxTemp)
      if second % 60 == 0 then println(s"Simulated $second seconds (frame $second)")
      if next.maxDelta(state) < convergenceThreshold then
        converged = true
        println(s"Converged at $second seconds.")
      state = next
      second += 1

    println(s"Done. ${second} frames written to ${outDir.getPath}")
