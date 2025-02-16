package cheerpj_scala

import org.scalafmt.config.FormatEvent
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.internal.BestFirstSearch
import org.scalafmt.internal.FormatOps
import org.scalafmt.internal.FormatWriter
import scala.meta.inputs.Input
import scala.scalajs.js.annotation._

@JSExportTopLevel("Scalafmt")
object Scalafmt {
  @JSExport
  def format(source: String): String = {
    val style = ScalafmtConfig.default
    val runner = style.runner
    val tree = runner.parse(Input.VirtualFile("<input>", source)).get
    implicit val formatOps: FormatOps = new FormatOps(tree, style)
    runner.event(FormatEvent.CreateFormatOps(formatOps))
    implicit val formatWriter: FormatWriter = new FormatWriter(formatOps)
    val res = BestFirstSearch(Set.empty)
    val x = formatWriter.mkString(res.state)
    println(s"formatted ${source == x}")
    x
  }
}
