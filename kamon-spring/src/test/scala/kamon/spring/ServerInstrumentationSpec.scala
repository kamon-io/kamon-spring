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

import java.time.temporal.ChronoUnit

import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.spring.client.HttpClientSupport
import kamon.spring.webapp.AppSupport
import kamon.spring.webapp.controller.SyncTracingController
import kamon.trace.Span
import kamon.trace.Span.TagValue
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.concurrent.duration._

class ServerInstrumentationSpec extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with OptionValues
  with SpanReporter
  with AppSupport
  with HttpClientSupport {

  override protected def beforeAll(): Unit = {
    Kamon.reconfigure(ConfigFactory.load())
    startApp()
    startRegistration()
  }

  override protected def afterAll(): Unit = {
    stopApp()
    stopRegistration()
  }

  "The Server instrumentation on Sync Spring Boot" should {
    "propagate the current context and respond to the ok endpoint" in {

      get("/sync/tracing/ok").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "sync.tracing.ok.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/ok"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe ""
      }
    }

    "propagate the current context and respond to the not-found endpoint" in {

      get("/sync/tracing/not-found").getStatusLine.getStatusCode shouldBe 404

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "not-found"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/not-found"
        span.tags("http.status_code") shouldBe TagValue.Number(404)

        span.context.parentID.string shouldBe ""
      }
    }

    "propagate the current context and respond to the error endpoint" in {
      get("/sync/tracing/error").getStatusLine.getStatusCode shouldBe 500

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "sync.tracing.error.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/error"
        span.tags("error") shouldBe TagValue.True
        span.tags("http.status_code") shouldBe TagValue.Number(500)

        span.context.parentID.string shouldBe ""
      }
    }

    "propagate the current context and respond to the exception endpoint produced with abnormal termination" in {
      get("/sync/tracing/exception").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {
        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "sync.tracing.exception.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/exception"
        span.tags("error") shouldBe TagValue.True
        span.tags("http.status_code") shouldBe TagValue.Number(500)
      }
    }
    "propagate the current context and respond to the slowly endpoint" in {

      get("/sync/tracing/slowly").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "sync.tracing.slowly.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/slowly"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe ""

        span.from.until(span.to, ChronoUnit.MILLIS) shouldBe >= (SyncTracingController.slowlyServiceDuration.toMillis)
      }
    }

    "resume the incoming context and respond to the ok endpoint" in {
      get("/sync/tracing/ok", IncomingContext.headersB3).getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = reporter.nextSpan().value
        val spanTags = stringTag(span) _

        span.operationName shouldBe "sync.tracing.ok.get"
        spanTags("span.kind") shouldBe "server"
        spanTags("component") shouldBe "spring-server"
        spanTags("http.method") shouldBe "GET"
        spanTags("http.url") shouldBe "/sync/tracing/ok"
        span.tags("http.status_code") shouldBe TagValue.Number(200)

        span.context.parentID.string shouldBe IncomingContext.SpanId
        span.context.traceID.string shouldBe IncomingContext.TraceId
      }
    }
  }

  def stringTag(span: Span.FinishedSpan)(tag: String): String = {
    span.tags(tag).asInstanceOf[TagValue.String].string
  }

  private object IncomingContext {
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
