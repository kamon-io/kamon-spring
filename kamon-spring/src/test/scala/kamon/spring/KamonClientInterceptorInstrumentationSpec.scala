package kamon.spring

import kamon.spring.utils.ForkTest
import kamon.testkit.Reconfigure
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.springframework.web.client.{AsyncRestTemplate, RestTemplate}


@ForkTest(
  attachKanelaAgent = true,
  extraJvmOptions = "-Dkanela.modules.spring-module.instrumentations.0=" +
    "kamon.spring.instrumentation.ClientInstrumentation ")
class KamonClientInterceptorInstrumentationSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Eventually
  with OptionValues
  with SpanReporter
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
        |      includes = [ "http**" ]
        |      excludes = [ "sync**", "async**" ]
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

  "A RestTemplate client built by default constructor" should behave like contextPropagation(SyncClientProvider())
  "A RestTemplate client built by Builder" should behave like contextPropagation(SyncClientProvider())
  "A AsyncRestTemplate client built by default constructor" should behave like contextPropagation(AsyncClientProvider())

  case class SyncClientProvider() extends ClientProvider.Sync {
    override lazy val restTemplate: RestTemplate = new RestTemplate()
  }

  case class AsyncClientProvider() extends ClientProvider.Async {
    override lazy val asyncRestTemplate: AsyncRestTemplate = new AsyncRestTemplate()
  }
}
