package cheerpj_scala

import unfiltered.jetty.Server
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ContentType
import unfiltered.response.HtmlContent
import unfiltered.response.JsContent
import unfiltered.response.JsonContent
import unfiltered.response.NotFound
import unfiltered.response.Ok
import unfiltered.response.ResponseBytes
import unfiltered.response.ResponseString
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TestServer {
  object AsInt {
    def unapply(s: String): Option[Int] = Option(s.toInt)
  }

  private val JarContentType = ContentType(
    "application/java-archive"
  )

  private val AcceptRangesBytes = unfiltered.response.AcceptRanges("bytes")

  val DefaultPath: String = "cheerpj-scala"

  def server(log: Boolean): Server = {
    unfiltered.jetty.Server.anylocal.plan(
      unfiltered.filter.Planify {
        case Path("/favicon.ico") =>
          Ok
        case req @ Path(Seg(DefaultPath :: pp)) =>
          val p = pp.mkString("/")
          val response = {
            if (log) {
              println(
                s"${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())} ${p} ${req.headers("Range").mkString(" ")}"
              )
            }
            val f = new File("sources", p)
            if (f.isFile) {
              val path = f.toPath
              lazy val bytes = Files.readAllBytes(path)
              lazy val resString = ResponseString(new String(bytes, "UTF-8"))
              f.getName
                .split('.')
                .lastOption
                .collect {
                  case "html" =>
                    HtmlContent ~> resString
                  case "js" =>
                    JsContent ~> resString
                  case "json" =>
                    JsonContent ~> resString
                  case "jar" =>
                    val prefix = "bytes="
                    Option(req.headers("Range").toList).collect {
                      case List(str) if str.startsWith(prefix) =>
                        str.drop(prefix.length).split('-')
                    }.collect { case Array(AsInt(start), AsInt(end)) =>
                      val buffer = new Array[Byte](end - start + 1)
                      val randomAccessFile = new RandomAccessFile(f, "r")
                      try {
                        randomAccessFile.seek(start)
                        randomAccessFile.readFully(buffer)
                      } finally {
                        randomAccessFile.close()
                      }
                      unfiltered.response.ContentRange(
                        s"bytes ${start}-${end}/${f.length}"
                      ) ~> unfiltered.response.PartialContent ~> JarContentType ~> ResponseBytes(
                        buffer
                      )
                    }.getOrElse {
                      JarContentType ~> ResponseBytes(
                        bytes
                      )
                    }
                }
                .getOrElse {
                  println(s"other extension ${p}")
                  HtmlContent ~> resString
                }
            } else {
              println(s"not found ${p}")
              NotFound
            }
          }

          AcceptRangesBytes ~> response
      }
    )
  }

  def main(args: Array[String]): Unit = {
    server(log = true).run { svr =>
      unfiltered.util.Browser.open(s"${svr.portBindings.head.url}/$DefaultPath/index.html")
    }
  }
}
