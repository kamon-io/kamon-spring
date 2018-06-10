package kamon.spring

import kamon.Kamon
import kamon.context.Context
import kamon.spring.client.interceptor.KamonRestTemplateInterceptor
import kamon.spring.webapp.AppSupport
import kamon.testkit.Reconfigure
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.{HttpClientErrorException, HttpServerErrorException, RestTemplate}

import scala.concurrent.duration._


class ClientInstrumentationSpec extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with Eventually
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
        |      includes = [ "sync**" ]
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
    val restTemplate: RestTemplate = new RestTemplate {
      import scala.collection.JavaConverters._
      this.setInterceptors(List[ClientHttpRequestInterceptor](new KamonRestTemplateInterceptor).asJava)
    }
  }

  "The Client instrumentation on Sync Rest Template" should {
    "propagate the current context and generate a span around an outgoing request" in new ClientFixture {

      private val okSpan = Kamon.buildSpan("ok-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/ok"
      Kamon.withContext(Context.create(Span.ContextKey, okSpan)) {
        restTemplate.getForEntity(url, classOf[String]).getStatusCodeValue shouldBe 200
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
      private val exc = intercept[HttpClientErrorException] {
        Kamon.withContext(Context.create(Span.ContextKey, notFoundSpan)) {
          restTemplate.getForEntity(url, classOf[String])
        }
      }
      exc.getStatusCode.value() shouldBe 404

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

      intercept[HttpServerErrorException] {
        Kamon.withContext(Context.create(Span.ContextKey, errorSpan)) {
          restTemplate.getForEntity(url, classOf[String])
        }
      }.getStatusCode.value() shouldBe 500

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
        restTemplate.getForEntity(url, classOf[String]).getStatusCodeValue shouldBe 200
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
