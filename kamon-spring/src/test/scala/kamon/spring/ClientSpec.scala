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

package kamon.spring

import kamon.spring.client.interceptor.{KamonAsyncInterceptorInjector, KamonSyncInterceptorInjector}
import kamon.testkit.Reconfigure
import kamon.testkit.{Reconfigure, TestSpanReporter}

import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.springframework.web.client.{AsyncRestTemplate, RestTemplate}


class ClientSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Eventually
  with OptionValues
  with TestSpanReporter
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
//    startRegistration()

  }

  override protected def afterAll(): Unit = {
//    stopRegistration()
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
