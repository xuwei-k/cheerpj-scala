package cheerpj_scala

import org.scalatest.freespec.AnyFreeSpec
import java.io.File
import sbt.io.IO
import scala.util.Random

class Test extends AnyFreeSpec {
  "classic main" in {
    IO.withTemporaryDirectory { dir =>
      val src = new File(dir, "A.scala")
      val str1, str2 = Random.alphanumeric.take(10).mkString
      IO.write(
        src,
        s"""|package example
            |
            |object A {
            |  def main(args: Array[String]): Unit = {
            |    println("$str1")
            |
            |    print("$str2")
            |  }
            |}
            |""".stripMargin
      )
      val classpath = Nil.getClass.getProtectionDomain.getCodeSource.getLocation.toString
      val result = Main.withConsole {
        Main.runMain(src.getCanonicalPath, s"${dir.getCanonicalPath}/", Array(classpath))
      }._1
      val expect = List(str1, str2).mkString("\n")
      assert(result == expect, s"${result} != ${expect}")
    }
  }

  "no args" in test { str =>
    s"""|package example
        |
        |object A {
        |  def main(): Unit = {
        |    print("$str")
        |  }
        |}
        |""".stripMargin
  }

  "not static" in test { str =>
    s"""|package example
        |
        |class A {
        |  def main(args: Array[String]): Unit = {
        |    print("$str")
        |  }
        |}
        |""".stripMargin
  }

  "no args and not static" in test { str =>
    s"""|package example
        |
        |class A {
        |  def main(): Unit = {
        |    print("$str")
        |  }
        |}
        |""".stripMargin
  }

  private def test(source: String => String) = {
    IO.withTemporaryDirectory { dir =>
      val src = new File(dir, "A.scala")
      val str = Random.alphanumeric.take(10).mkString
      IO.write(
        src,
        source(str)
      )
      val classpath = Nil.getClass.getProtectionDomain.getCodeSource.getLocation.toString
      val result = Main.withConsole {
        Main.runMain(src.getCanonicalPath, s"${dir.getCanonicalPath}/", Array(classpath))
      }._1
      assert(result == str, s"${result} != ${str}")
    }
  }

  "MainMethod sort" in {
    val values = List(
      MainMethod(isStatic = true, hasArgs = true),
      MainMethod(isStatic = true, hasArgs = false),
      MainMethod(isStatic = false, hasArgs = true),
      MainMethod(isStatic = false, hasArgs = false),
    )
    List
      .fill(30)(
        Random.shuffle(values)
      )
      .foreach { x =>
        assert(x.sorted == values)
      }
  }
}
