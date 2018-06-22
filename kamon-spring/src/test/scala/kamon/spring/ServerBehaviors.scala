package kamon.spring

import java.time.temporal.ChronoUnit

import kamon.spring.client.HttpClientTest
import kamon.trace.Span
import kamon.trace.Span.TagValue
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.concurrent.duration._

trait ServerProvider {
  def prefixEndpoint: String
  def port: Int
  def exceptionStatus: Int
  def slowlyServiceDuration: FiniteDuration
}

trait ServerBehaviors extends KamonSpringLogger {
  this: FlatSpec
    with Matchers
    with Eventually
    with OptionValues
    with SpanReporter =>

  def contextPropagation(app: ServerProvider): Unit = {
    val prefix = app.prefixEndpoint

    it should "propagate the current context and respond to the ok endpoint" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/ok").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe s"$prefix.tracing.ok.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/ok"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe ""
        reporter.nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the not-found endpoint" in {

      HttpClientTest(app.port).get(s"/$prefix/tracing/not-found").getStatusLine.getStatusCode shouldBe 404

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "not-found"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/not-found"
        span.tags("http.status_code") shouldBe TagValue.Number(404)

        span.context.parentID.string shouldBe ""
        reporter.nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the error endpoint" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/error").getStatusLine.getStatusCode shouldBe 500

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe s"$prefix.tracing.error.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/error"
        span.tags("error") shouldBe TagValue.True
        span.tags("http.status_code") shouldBe TagValue.Number(500)

        span.context.parentID.string shouldBe ""
        reporter.nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the controller with abnormal termination" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/exception").getStatusLine.getStatusCode shouldBe app.exceptionStatus

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe s"$prefix.tracing.exception.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/exception"
        span.tags("error") shouldBe TagValue.True
        span.tags("http.status_code") shouldBe TagValue.Number(500)

        span.context.parentID.string shouldBe ""
        reporter.nextSpan() shouldBe empty
      }
    }

    it should "propagate the current context and respond to the slowly endpoint" in {

      HttpClientTest(app.port).get(s"/$prefix/tracing/slowly").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(5 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe s"$prefix.tracing.slowly.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/slowly"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe ""
        span.from.until(span.to, ChronoUnit.MILLIS) shouldBe >= (app.slowlyServiceDuration.toMillis)
        reporter.nextSpan() shouldBe empty
      }
    }

    it should "resume the incoming context and respond to the ok endpoint" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/ok", IncomingContext.headersB3).getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe s"$prefix.tracing.ok.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe s"/$prefix/tracing/ok"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe IncomingContext.SpanId
        span.context.traceID.string shouldBe IncomingContext.TraceId
        reporter.nextSpan() shouldBe empty
      }
    }

    def stringTag(span: Span.FinishedSpan)(tag: String): String = {
      span.tags(tag).asInstanceOf[TagValue.String].string
    }

    object IncomingContext {
      import kamon.trace.SpanCodec.B3.{Headers => B3Headers}

      val TraceId = "1234"
      val ParentSpanId = "2222"
      val SpanId = "4321"
      val Sampled = "1"
      val Flags = "some=baggage;more=baggage"


      val headersB3 = Seq(
        (B3Headers.TraceIdentifier, TraceId),
        (B3Headers.ParentSpanIdentifier, ParentSpanId),
        (B3Headers.SpanIdentifier, SpanId),
        (B3Headers.Sampled, Sampled),
        (B3Headers.Flags, Flags))
    }
  }
}
