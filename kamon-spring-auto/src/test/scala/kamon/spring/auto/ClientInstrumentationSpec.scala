package kamon.spring.auto

import kamon.Kamon
import kamon.context.Context
import kamon.spring.auto.webapp.AppSupport
import kamon.testkit.Reconfigure
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.client._

import scala.concurrent.duration._
import scala.reflect.ClassTag


class ClientInstrumentationSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Eventually
  with OptionValues
  with SpanReporter
  with AppSupport
  with Reconfigure
  with ClientBehaviors {

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
        |      includes = [ "**" ]
        |    }
        |  }
        |}
        |
    """.stripMargin
    )
    startRegistration()

  }

  override protected def afterAll(): Unit = {
    stopRegistration()
  }

  "A RestTemplate client built by default constructor" should behave like contextPropagation(SyncClientProvider("restTemplateDefault"))
  "A RestTemplate client built by Builder" should behave like contextPropagation(SyncClientProvider("restTemplateByBuilder"))
  "A AsyncRestTemplate client built by default constructor" should behave like contextPropagation(AsyncClientProvider("asyncRestTemplateDefault"))

}

trait ClientProvider {
  def port: Int
  def GetRequest[T: ClassTag](url: String): ResponseEntity[T]

  def validateError[T <: HttpStatusCodeException: ClassTag](exception: Throwable, validatorF: Int => Unit): Unit
}

case class SyncClientProvider(restTemplateName: String) extends ClientProvider with AppSupport {
  import Inside._

  private lazy val app: ConfigurableApplicationContext = startApp(kamonSpringWebEnabled = false)
  private lazy val restTemplate: RestTemplate = app.getBean(restTemplateName).asInstanceOf[RestTemplate]

  override lazy val port: Int = {
    restTemplate // force initialization
    super.port
  }

  def GetRequest[T: ClassTag](url: String): ResponseEntity[T] =
    restTemplate.getForEntity(url, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

  def validateError[T <: HttpStatusCodeException: ClassTag](exception: Throwable, validatorF: Int => Unit): Unit = {
    inside(exception) { case ex: T =>
      validatorF(ex.getStatusCode.value())
    }
  }
}

case class AsyncClientProvider(restTemplateName: String) extends ClientProvider with AppSupport {
  import Inside._

  private lazy val app: ConfigurableApplicationContext = startApp(kamonSpringWebEnabled = false)
  private lazy val restTemplate: AsyncRestTemplate = app.getBean(restTemplateName).asInstanceOf[AsyncRestTemplate]

  override lazy val port: Int = {
    restTemplate // force initialization
    super.port
  }

  def GetRequest[T: ClassTag](url: String): ResponseEntity[T] =
    restTemplate.getForEntity(url, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).get()

  def validateError[T <: HttpStatusCodeException: ClassTag](exception: Throwable, validatorF: Int => Unit): Unit = {
    inside(exception.getCause) { case ex: T =>
      validatorF(ex.getStatusCode.value())
    }
  }
}

trait ClientBehaviors { this: FlatSpec
  with Matchers
  with Eventually
  with OptionValues
  with SpanReporter =>

  def contextPropagation(app: ClientProvider): Unit = {
    val port = app.port

    it should "propagate the current context and generate a span around an outgoing request" in {

      val okSpan = Kamon.buildSpan("ok-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/ok"
      Kamon.withContext(Context.create(Span.ContextKey, okSpan)) {
        app.GetRequest[String](url).getStatusCodeValue shouldBe 200
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

    it should "propagate the current context and generate a span called not-found around an outgoing request that produced a 404" in {

      val notFoundSpan = Kamon.buildSpan("not-found-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/not-found"
      val exc = intercept[Throwable] {
        Kamon.withContext(Context.create(Span.ContextKey, notFoundSpan)) {
          app.GetRequest[String](url)
        }
      }
      app.validateError[HttpClientErrorException](exc, _ shouldBe 404)

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

    it should "propagate the current context and generate a span with error around an outgoing request that produced a 500" in {

      val errorSpan = Kamon.buildSpan("error-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/error"

      val exc = intercept[Throwable] {
        Kamon.withContext(Context.create(Span.ContextKey, errorSpan)) {
          app.GetRequest[String](url)
        }
      }

      app.validateError[HttpServerErrorException](exc, _ shouldBe 500)

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

    it should "propagate the current context and pickup a SpanCustomizer and apply it to the new span and complete the outgoing request" in {

      val okSpan = Kamon.buildSpan("ok-operation-span").start()

      val url = s"http://localhost:$port/sync/tracing/ok"

      val customizedOperationName = "customized-operation-name"

      val context = Context.create(Span.ContextKey, okSpan)
        .withKey(SpanCustomizer.ContextKey, SpanCustomizer.forOperationName(customizedOperationName))

      Kamon.withContext(context) {
        app.GetRequest[String](url).getStatusCodeValue shouldBe 200
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
