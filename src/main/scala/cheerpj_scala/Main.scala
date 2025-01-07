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

  private def isMain(method: Method): Boolean = {
    (
      "main" == method.getName
    ) && Modifier.isStatic(
      method.getModifiers
    ) && (
      method.getReturnType == java.lang.Void.TYPE
    ) && (
      method.getParameters.map(_.getType).toList == List(classOf[Array[String]])
    )
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

  def runMain(scalaFile: String, outputDir: String, classpath: Array[String]): String = try {
    new File(outputDir).mkdirs()
    val (success, consoleOut) = scalac(scalaFile, "-d", outputDir, "-classpath", classpath.mkString(":"))
    if (!success) {
      consoleOut
    } else {
      val classNames = findClasses(outputDir)
      val url = new File(outputDir).toURI.toURL
      val loader = new URLClassLoader(Array[URL](url))
      val hasMain = classNames.filter { className =>
        loader.loadClass(className).getMethods.exists(isMain)
      }.sorted.headOption
      hasMain.fold("") { className =>
        val clazz = loader.loadClass(className)
        val mainMethod = clazz.getMethod("main", classOf[Array[String]])
        mainMethod.setAccessible(true)
        val args = Array.empty[String]
        time("run") {
          withConsole(
            mainMethod.invoke(null, args)
          )._2
        }
      }
    }
  } catch {
    case e: Throwable =>
      stacktraceString(e)
  }

  private def withConsole[A](f: => A): (A, String) = {
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
    print(s"${label} = ${System.currentTimeMillis() - start}")
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
