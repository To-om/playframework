/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.io.IOException
import java.nio.file.{ Files, Paths }

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.http.{ HttpEntity, HttpErrorHandler }
import play.core.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.Random

class MultipartBodyParserSpec extends Specification {

  "Multipar body parser" should {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val playBodyParsers = PlayBodyParsers(
      tfc = new InMemoryTemporaryFileCreator(10))

    "return an error if temporary file creation fails" in {

      val fileSize = 100
      val boundary = "-----------------------------14568445977970839651285587160"
      val header =
        s"--$boundary\r\n" +
          "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"uploadedfile.txt\"\r\n" +
          "Content-Type: application/octet-stream\r\n" +
          "\r\n"
      val content = Array.ofDim[Byte](fileSize)
      val footer =
        "\r\n" +
          "\r\n" +
          s"--$boundary--\r\n"

      val body = Source(
        ByteString(header) ::
          ByteString(content) ::
          ByteString(footer) ::
          Nil)

      val bodySize = header.length + fileSize + footer.length

      val request = FakeRequest(
        method = "POST",
        uri = "/x",
        headers = FakeHeaders(Seq(
          "Content-Type" -> s"multipart/form-data; boundary=$boundary",
          "Content-Length" -> bodySize.toString)),
        body = body)

      val response = playBodyParsers.multipartFormData.apply(request).run(body)
      Await.result(response, Duration.Inf) must throwA[IOException]
    }
  }
}
