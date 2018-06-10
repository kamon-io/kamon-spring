package kamon.spring

import java.util.concurrent.ExecutionException

import kamon.Kamon
import kamon.context.Context
import kamon.spring.client.interceptor.KamonAsyncRestTemplateInterceptor
import kamon.spring.webapp.AppSupport
import kamon.testkit.Reconfigure
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import org.scalatest.concurrent.Eventually
import org.scalatest._
import org.springframework.http.client.AsyncClientHttpRequestInterceptor
import org.springframework.web.client.{AsyncRestTemplate, HttpClientErrorException, HttpServerErrorException}

import scala.concurrent.duration._


class AsyncClientInstrumentationSpec extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with Inside
  with OptionValues
  with SpanReporter
  with AppSupport
  with Reconfigure {

  override protected def beforeAll(): Unit = {
    applyConfig(
      """
        |kamon {
        |  metric.tick-interval = 10 millis
        |  trace.tick-interval = 10 millis
        |  trace.sampler = "always"
        |
        |  util.filters {
        |    span-filter {
        |      includes = [ "async**" ]
        |    }
        |  }
        |}
        |
    """.stripMargin
    )
    startApp(enableKamon = false)
    startRegistration()

  }

  override protected def afterAll(): Unit = {
    stopApp()
    stopRegistration()
  }

  trait ClientFixture {
    val restTemplate: AsyncRestTemplate = new AsyncRestTemplate {
      import scala.collection.JavaConverters._
      this.setInterceptors(List[AsyncClientHttpRequestInterceptor](new KamonAsyncRestTemplateInterceptor).asJava)
    }
  }

  "The Client instrumentation on Async Rest Template" should {
    "propagate the current context and generate a span around an outgoing request" in new ClientFixture {

      private val okSpan = Kamon.buildSpan("ok-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/ok"
      Kamon.withContext(Context.create(Span.ContextKey, okSpan)) {
        restTemplate.getForEntity(url, classOf[String]).get().getStatusCodeValue shouldBe 200
      }

      eventually(timeout(3.seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe url
        spanTags("span.kind") shouldBe "client"
        spanTags("component") shouldBe "spring-rest-template-client"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe url
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe okSpan.context().spanID.string
        reporter.nextSpan() shouldBe None
      }
    }

    "propagate the current context and generate a span called not-found around an outgoing request that produced a 404" in new ClientFixture {

      private val notFoundSpan = Kamon.buildSpan("not-found-operation-span").start()

      private val url = s"http://localhost:$port/sync/tracing/not-found"

      private val externalExc = intercept[ExecutionException] {
        Kamon.withContext(Context.create(Span.ContextKey, notFoundSpan)) {
          restTemplate.getForEntity(url, classOf[String]).get()
        }
      }

      inside(externalExc.getCause) { case ex: HttpClientErrorException =>
        ex.getStatusCode.value() shouldBe 404
      }

      eventually(timeout(3.seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "not-found"
        spanTags("span.kind") shouldBe "client"
        spanTags("component") shouldBe "spring-rest-template-client"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe url
        span.tags("http.status_code") shouldBe TagValue.Number(404)

        span.context.parentID.string shouldBe notFoundSpan.context().spanID.string
        reporter.nextSpan() shouldBe None
      }
    }

    "propagate the current context and generate a span with error around an outgoing request that produced a 500" in new ClientFixture {

      private val errorSpan = Kamon.buildSpan("error-operation-span").start()

      private val url = s"http://localhost:$port/sync/tracing/error"

      private val externalExc = intercept[ExecutionException] {
        Kamon.withContext(Context.create(Span.ContextKey, errorSpan)) {
          restTemplate.getForEntity(url, classOf[String]).get()
        }
      }

      inside(externalExc.getCause) { case ex: HttpServerErrorException =>
        ex.getStatusCode.value() shouldBe 500
      }

      eventually(timeout(3.seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe url
        spanTags("span.kind") shouldBe "client"
        spanTags("component") shouldBe "spring-rest-template-client"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe url
        span.tags("error") shouldBe TagValue.True
        span.tags("http.status_code") shouldBe TagValue.Number(500)

        span.context.parentID.string shouldBe errorSpan.context().spanID.string
        reporter.nextSpan() shouldBe None
      }
    }

    "propagate the current context and pickup a SpanCustomizer and apply it to the new span and complete the outgoing request" in new ClientFixture {

      private val okSpan = Kamon.buildSpan("ok-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/ok"

      val customizedOperationName = "customized-operation-name"

      private val context = Context.create(Span.ContextKey, okSpan)
        .withKey(SpanCustomizer.ContextKey, SpanCustomizer.forOperationName(customizedOperationName))

      Kamon.withContext(context) {
        restTemplate.getForEntity(url, classOf[String]).get().getStatusCodeValue shouldBe 200
      }

      eventually(timeout(3.seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe customizedOperationName
        spanTags("span.kind") shouldBe "client"
        spanTags("component") shouldBe "spring-rest-template-client"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe url
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe okSpan.context().spanID.string
        reporter.nextSpan() shouldBe None
      }
    }
  }

  def stringTag(span: Span.FinishedSpan)(tag: String): String = {
    span.tags(tag).asInstanceOf[TagValue.String].string
  }

}
