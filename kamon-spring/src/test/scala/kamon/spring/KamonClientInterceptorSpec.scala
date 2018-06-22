package kamon.spring

import kamon.spring.client.interceptor.{KamonAsyncInterceptorInjector, KamonSyncInterceptorInjector}
import kamon.testkit.Reconfigure
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.springframework.web.client.{AsyncRestTemplate, RestTemplate}


class KamonClientInterceptorSpec extends FlatSpec
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

  "A RestTemplate client built by default constructor" should behave like contextPropagation(SyncClientProvider())
  "A RestTemplate client built by Builder" should behave like contextPropagation(SyncClientProvider())
  "A AsyncRestTemplate client built by default constructor" should behave like contextPropagation(AsyncClientProvider())

  case class SyncClientProvider() extends ClientProvider.Sync {
    override lazy val restTemplate: RestTemplate = {
      val restTemplate = new RestTemplate()
      KamonSyncInterceptorInjector.register(restTemplate)
      restTemplate
    }
  }

  case class AsyncClientProvider() extends ClientProvider.Async {
    override lazy val asyncRestTemplate: AsyncRestTemplate = {
      val restTemplate = new AsyncRestTemplate()
      KamonAsyncInterceptorInjector.register(restTemplate)
      restTemplate
    }
  }

}
