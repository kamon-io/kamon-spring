/*
 * =========================================================================================
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

import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.spring.utils.ForkTest
import kamon.spring.webapp.AppSupport
import kamon.spring.webapp.controller.{AsyncTracingController, SyncTracingController}
import kamon.testkit.Reconfigure
import org.scalatest._
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration.FiniteDuration

@ForkTest(attachKanelaAgent = true)
class JettyServerInstrumentationByKanelaSpec extends ServerInstrumentationSpec {
  override def startApp(): Unit = startJettyApp(kamonSpringWebEnabled = false)
}

@ForkTest(attachKanelaAgent = true)
class TomcatServerInstrumentationByKanelaSpec extends ServerInstrumentationSpec {
  override def startApp(): Unit = startTomcatApp(kamonSpringWebEnabled = false)
}

@ForkTest(attachKanelaAgent = true)
class UndertowServerInstrumentationByKanelaSpec extends ServerInstrumentationSpec {
  override def startApp(): Unit = startUndertowApp(kamonSpringWebEnabled = false)
}

abstract class ServerInstrumentationByKanelaSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with OptionValues
  with SpanReporter
  with Reconfigure
  with AppSupport
  with ServerBehaviors { self =>

  def startApp(): Unit

  override protected def beforeAll(): Unit = {
    Kamon.reconfigure(ConfigFactory.load())
    startApp()
    startRegistration()
  }

  override protected def afterAll(): Unit = {
    stopApp()
    stopRegistration()
  }

  class Server(override val prefixEndpoint: String,
               override val exceptionStatus: Int,
               override val slowlyServiceDuration: FiniteDuration) extends ServerProvider {
    override def port: Int = self.port
  }

  "A Server with sync controllers instrumented by Kanela" should behave like
    contextPropagation(new Server(prefixEndpoint = "sync", exceptionStatus = 200,
      SyncTracingController.slowlyServiceDuration))
  "A Server with async controllers instrumented by Kanela" should behave like
    contextPropagation(new Server(prefixEndpoint = "async", exceptionStatus = 500,
      AsyncTracingController.slowlyServiceDuration))

}
