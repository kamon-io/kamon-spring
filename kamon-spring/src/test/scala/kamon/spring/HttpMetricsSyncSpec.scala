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

import java.util.concurrent.Executors

import kamon.Kamon
import kamon.instrumentation.http.HttpServerMetrics
import kamon.servlet.Servlet
import kamon.spring.client.HttpClientSupport
import kamon.spring.webapp.AppSupport
import kamon.testkit.{InstrumentInspection, MetricInspection, TestSpanReporter}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.concurrent.{Await, ExecutionContext, Future}


class HttpMetricsSyncSpec extends WordSpec
  with Matchers
  with Eventually
  with SpanSugar
  with InstrumentInspection.Syntax
  with OptionValues
  with TestSpanReporter
  with BeforeAndAfterAll
  with AppSupport
  with HttpClientSupport {

  override protected def beforeAll(): Unit = {
    applyConfig(
      """
        |kamon {
        |  metric.tick-interval = 10 millis
        |  servlet.metrics.enabled = true
        |}
    """.stripMargin)
    startJettyApp()
//    testSpanReporter()
//    startRegistration()
  }

  override protected def afterAll(): Unit = {
//    stopRegistration()
//    testSpanReporter().stop()
    stopApp()
    Await.result(Kamon.stopModules(), 2 seconds)
  }

  private val parallelRequestExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(15))

  def serverInstruments(): HttpServerMetrics.HttpServerInstruments = HttpServerMetrics.of(Servlet.tags.serverComponent, Servlet.server.interface, Servlet.server.port)

  "The Http Metrics generation" should {
    "track the total of active requests" in {
      for(_ <- 1 to 10) {
        Future { get("/sync/tracing/slowly").close() }(parallelRequestExecutor)
      }

      eventually(timeout(5 seconds)) {
        serverInstruments().activeRequests.distribution().max should (be > 0L and be <= 10L)
      }

      eventually(timeout(3 seconds)) {
        serverInstruments().activeRequests.distribution().min should (be >= 0L and be <= 10L)
      }
      testSpanReporter().clear()
    }

    "track the response time with status code 2xx" in {
      for(_ <- 1 to 100) get("/sync/tracing/ok").close()
//      ResponseTimeMetrics().forStatusCode("2xx").distribution().max should be > 0L
    }

    "track the response time with status code 4xx" in {
      for(_ <- 1 to 100) get("/sync/tracing/not-found").close()
//      ResponseTimeMetrics().forStatusCode("4xx").distribution().max should be > 0L
    }

    "track the response time with status code 5xx" in {
      for(_ <- 1 to 100) get("/sync/tracing/error").close()
//      ResponseTimeMetrics().forStatusCode("5xx").distribution().max should be > 0L
    }
  }
}
