package cheerpj_scala

import org.scalatest.freespec.AnyFreeSpec
import java.io.File
import sbt.io.IO
import scala.util.Random

class Test extends AnyFreeSpec {
  "test" in {
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
      val result = Main.runMain(src.getCanonicalPath, s"${dir.getCanonicalPath}/", Array(classpath))
      val expect = List(str1, str2).mkString("\n")
      assert(result == expect, s"${result} != ${expect}")
    }
  }
}
