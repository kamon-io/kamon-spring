/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.spring.auto

import kamon.spring.auto.webapp.AppSupport
import kamon.spring.utils.SpanReporter
import kamon.spring.{ClientBehaviors, ClientProvider}
import kamon.testkit.Reconfigure
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.client._

import scala.reflect.ClassTag


class ClientSpec extends FlatSpec
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

  "A RestTemplate client built by default constructor" should behave like
    contextPropagation(ClientAutoconfigureProvider.Sync("restTemplateDefault"))
  "A RestTemplate client built by Builder" should behave like
    contextPropagation(ClientAutoconfigureProvider.Sync("restTemplateByBuilder"))
  "An AsyncRestTemplate client built by default constructor" should behave like
    contextPropagation(ClientAutoconfigureProvider.Async("asyncRestTemplateDefault"))
}

object ClientAutoconfigureProvider {

  case class Sync(restTemplateName: String) extends ClientProvider with AppSupport {
    import Inside._

    protected lazy val app: ConfigurableApplicationContext = startApp(kamonWebEnabled = false)

    def restTemplate: RestTemplate = app.getBean(restTemplateName).asInstanceOf[RestTemplate]

    override lazy val port: Int = {
      app // force initialization
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

  case class Async(restTemplateName: String) extends ClientProvider with AppSupport {
    import Inside._

    protected lazy val app: ConfigurableApplicationContext = startApp(kamonWebEnabled = false)

    def asyncRestTemplate: AsyncRestTemplate = app.getBean(restTemplateName).asInstanceOf[AsyncRestTemplate]

    override lazy val port: Int = {
      app // force initialization
      super.port
    }

    def GetRequest[T: ClassTag](url: String): ResponseEntity[T] =
      asyncRestTemplate.getForEntity(url, implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).get()

    def validateError[T <: HttpStatusCodeException: ClassTag](exception: Throwable, validatorF: Int => Unit): Unit = {
      inside(exception.getCause) { case ex: T =>
        validatorF(ex.getStatusCode.value())
      }
    }
  }
}
