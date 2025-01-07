// latest scala version does not work with CheerpJ
scalaVersion := "2.12.3" // scala-steward:off

name := "cheerpj_scala"

TaskKey[Unit]("dist") := {
  val exclude: Set[String] = Set(
  )
  val jarFiles = (Compile / fullClasspathAsJars).value.map(_.data).filterNot(f => exclude(f.getName))
  val dir = file("sources") / "dist"
  IO.delete(dir)
  def jarName(x: File): String = {
    if (x.getName.contains('-')) {
      x.getName.split('-').dropRight(1).mkString("", "-", ".jar")
    } else {
      x.getName.replace('%', '-')
    }
  }
  jarFiles.foreach { jar =>
    IO.copyFile(jar, dir / jarName(jar))
  }
  IO.write(
    dir / "jar_files.js",
    jarFiles.map(x => s"""  "${jarName(x)}"""").sorted.mkString("export const jarNames = [\n", ",\n", "\n]\n")
  )
}

scalacOptions ++= Seq(
  "-deprecation",
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-parser" % "0.14.10",
  "io.circe" %% "circe-generic" % "0.14.10",
  "org.typelevel" %% "cats-core" % "2.12.0",
  "org.scalatest" %% "scalatest-freespec" % "3.2.19" % Test,
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-sbt" %% "io" % "1.10.4" % Test,
  "org.slf4j" % "slf4j-simple" % "2.0.16" % Test,
  "ws.unfiltered" %% "unfiltered-filter" % "0.10.4" % Test, // scala-steward:off
  "ws.unfiltered" %% "unfiltered-jetty" % "0.10.4" % Test, // scala-steward:off
)

fork / run := true
run / fork := true
Test / fork := true
