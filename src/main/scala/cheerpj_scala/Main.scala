package cheerpj_scala

import scala.collection.JavaConverters._
import java.io.PrintWriter
import java.io.StringWriter
import java.io.PrintStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.stream.Collectors

object Main {

  private def classSuffix = ".class"

  /**
   * [[https://openjdk.org/jeps/445]]
   */
  private def getMainMethod(method: Method, clazz: Class[_]): Option[MainMethod] = {
    if (
      (
        "main" == method.getName
      ) && (
        method.getReturnType == java.lang.Void.TYPE
      )
    ) {
      val hasArgs = method.getParameters.map(_.getType).toList == List(classOf[Array[String]])
      val emptyArg = method.getParameterCount == 0
      if (Modifier.isStatic(method.getModifiers)) {
        (hasArgs, emptyArg) match {
          case (true, _) =>
            Some(MainMethod(isStatic = true, hasArgs = true))
          case (_, true) =>
            Some(MainMethod(isStatic = true, hasArgs = false))
          case _ =>
            None
        }
      } else if (clazz.getConstructors.exists(_.getParameterCount == 0)) {
        (hasArgs, emptyArg) match {
          case (true, _) =>
            Some(MainMethod(isStatic = false, hasArgs = true))
          case (_, true) =>
            Some(MainMethod(isStatic = false, hasArgs = false))
          case _ =>
            None
        }
      } else {
        None
      }
    } else {
      None
    }
  }

  private def findClasses(dirName: String): List[String] = {
    Files
      .find(
        new File(dirName).toPath,
        20,
        (f, x) => x.isRegularFile && f.toFile.getName.endsWith(classSuffix)
      )
      .collect(Collectors.toList())
      .asScala
      .map { f =>
        f.toFile.getCanonicalPath.drop(dirName.length).replace('/', '.').dropRight(classSuffix.length)
      }
      .toList
  }

  def info(value: String): Unit = (new hoge.Hoge).info(value)

  def runMain(scalaFile: String, outputDir: String, classpath: Array[String]): String = try {
    new File(outputDir).mkdirs()
    val (success, consoleOut) = scalac(scalaFile, "-d", outputDir, "-classpath", classpath.mkString(":"))
    if (!success) {
      consoleOut
    } else {
      val classNames = findClasses(outputDir)
      val url = new File(outputDir).toURI.toURL
      val loader = new URLClassLoader(Array[URL](url))
      val classAndMain = classNames.flatMap { className =>
        val clazz = loader.loadClass(className)
        clazz.getMethods.flatMap(m => getMainMethod(m, clazz)).map(clazz -> _)
      }.sortBy(x => (x._2, x._1.getName)).headOption
      classAndMain.fold("not found main method") { case (clazz, mainMethodInfo) =>
        val mainMethod = {
          if (mainMethodInfo.hasArgs) {
            clazz.getMethod("main", classOf[Array[String]])
          } else {
            clazz.getMethod("main")
          }
        }
        mainMethod.setAccessible(true)
        val args = Array.empty[String]
        time("run") {
          withConsole {
            val instance: Any = {
              if (mainMethodInfo.isStatic) {
                null
              } else {
                clazz.getConstructor().newInstance()
              }
            }
            if (mainMethodInfo.hasArgs) {
              mainMethod.invoke(instance, args)
            } else {
              mainMethod.invoke(instance)
            }
          }._2
        }
      }
    }
  } catch {
    case e: Throwable =>
      stacktraceString(e)
  }

  def withConsole[A](f: => A): (A, String) = {
    val out = new ByteArrayOutputStream
    val s = new PrintStream(out)
    val result = Console.withErr(s) {
      Console.withOut(s) {
        withStdOut(s) {
          withStdErr(s)(f)
        }
      }
    }
    s.flush()
    result -> out.toString(StandardCharsets.UTF_8.name)
  }

  private def withStdErr[A](out: PrintStream)(f: => A): A = {
    val original = System.err
    try {
      System.setErr(out)
      f
    } finally {
      System.setErr(original)
    }
  }

  private def withStdOut[A](out: PrintStream)(f: => A): A = {
    val original = System.out
    try {
      System.setOut(out)
      f
    } finally {
      System.setOut(original)
    }
  }

  private def time[A](label: String)(f: => A): A = {
    val start = System.currentTimeMillis()
    val res = f
    info(s"${label} = ${System.currentTimeMillis() - start} ms")
    res
  }

  def scalac(args: String*): (Boolean, String) = {
    time("compile") {
      withConsole(
        scala.tools.nsc.Main.process(args.toArray)
      )
    }
  }

  def stacktraceString(e: Throwable): String = {
    val s = new StringWriter
    val writer = new PrintWriter(s)
    e.printStackTrace(writer)
    writer.flush()
    s.flush()
    s.toString
  }

}
