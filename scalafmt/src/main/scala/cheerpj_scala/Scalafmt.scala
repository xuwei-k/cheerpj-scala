package cheerpj_scala

import scala.scalajs.js.annotation._

@JSExportTopLevel("Scalafmt")
object Scalafmt {
  @JSExport
  def format(source: String): String = {
    org.scalafmt.Scalafmt.format(source).get
  }
}
