lazy val cheerpjScala = project
  .in(file("."))
  .settings(
    scalaVersion := {
      // latest scala version does not work with CheerpJ
      "2.12.3" // scala-steward:off
    },
    name := "cheerpj_scala",
    TaskKey[Unit]("dist") := {
      val dir = file("sources") / "dist"
      IO.delete(dir)
      val Seq(m) = (scalafmt / Compile / fullLinkJS).value.data.publicModules.toSeq
      val src = (scalafmt / Compile / fullLinkJSOutput).value
      val f = src / m.jsFileName
      val srcMap = src / m.sourceMapName.getOrElse(sys.error("source map not found"))
      IO.copyFile(f, dir / m.jsFileName)
      IO.copyFile(srcMap, dir / srcMap.getName)

      val exclude: Set[String] = Set(
      )
      val jarFiles = (Compile / fullClasspathAsJars).value.map(_.data).filterNot(f => exclude(f.getName))
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
    },
    scalacOptions ++= Seq(
      "-deprecation",
    ),
    libraryDependencies ++= Seq(
      "org.scala-sbt" %% "io" % "1.10.4" % Test,
      "io.circe" %% "circe-parser" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "org.typelevel" %% "cats-free" % "2.12.0",
      "org.scalatest" %% "scalatest-freespec" % "3.2.19" % Test,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    ),
    Test / fork := true
  )
  .aggregate(testServer, scalafmt)

lazy val testServer = project
  .in(file("test-server"))
  .settings(
    scalaVersion := "2.13.15",
    libraryDependencies ++= Seq(
      "org.scala-sbt" %% "io" % "1.10.4",
      "org.slf4j" % "slf4j-simple" % "2.0.16",
      "ws.unfiltered" %% "unfiltered-filter" % "0.10.4", // scala-steward:off
      "ws.unfiltered" %% "unfiltered-jetty" % "0.10.4", // scala-steward:off
    )
  )

lazy val scalafmt = project
  .in(file("scalafmt"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "2.13.15",
    scalaJSLinkerConfig ~= {
      _.withESFeatures(_.withESVersion(org.scalajs.linker.interface.ESVersion.ES2018))
    },
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    fork / run := true,
    run / fork := true,
    libraryDependencies ++= Seq(
      ("com.github.xuwei-k" %%% "scalafmt-core" % "3.8.3-fork-2").withSources(),
    )
  )
