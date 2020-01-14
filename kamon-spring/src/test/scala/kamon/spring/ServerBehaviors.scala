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

import java.time.temporal.ChronoUnit

import kamon.spring.client.HttpClientTest
import kamon.tag.Lookups.{plain, plainLong, plainBoolean}
import kamon.testkit.TestSpanReporter
import kamon.trace.Span
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.concurrent.duration._

trait ServerProvider {
  def prefixEndpoint: String
  def port: Int
  def exceptionStatus: Int
  def slowlyServiceDuration: FiniteDuration
}

trait ServerBehaviors extends KamonSpringLogger {
  this: FlatSpec
    with Matchers
    with Eventually
    with OptionValues
    with TestSpanReporter =>

  def contextPropagation(app: ServerProvider): Unit = {
    val prefix = app.prefixEndpoint

    it should "propagate the current context and respond to the ok endpoint" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/ok").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = testSpanReporter().nextSpan().value

        span.operationName shouldBe s"$prefix.tracing.ok.get"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/ok"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 200

        span.parentId shouldBe ""

        testSpanReporter.nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the not-found endpoint" in {

      HttpClientTest(app.port).get(s"/$prefix/tracing/not-found").getStatusLine.getStatusCode shouldBe 404

      eventually(timeout(3 seconds)) {
        val span = testSpanReporter.nextSpan().value

        span.operationName shouldBe "not-found"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/not-found"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 404

        span.parentId shouldBe ""

        testSpanReporter().nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the error endpoint" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/error").getStatusLine.getStatusCode shouldBe 500

      eventually(timeout(3 seconds)) {
        val span = testSpanReporter.nextSpan().value

        span.operationName shouldBe "$prefix.tracing.error.get"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/error"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 500
        span.metricTags.get(plainBoolean("error")) shouldBe true

        span.parentId shouldBe ""

        testSpanReporter.nextSpan() shouldBe empty
      }
    }


    it should "propagate the current context and respond to the controller with abnormal termination" in {
      HttpClientTest(app.port).get(s"/$prefix/tracing/exception").getStatusLine.getStatusCode shouldBe app.exceptionStatus

      eventually(timeout(3 seconds)) {
        val span = testSpanReporter.nextSpan().value

        span.operationName shouldBe s"$prefix.tracing.exception.get"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/exception"
        span.metricTags.get(plainBoolean("error")) shouldBe true
        span.metricTags.get(plainLong("http.status_code")) shouldBe 500

        span.parentId shouldBe ""

        testSpanReporter.nextSpan() shouldBe empty
      }
    }

    it should "propagate the current context and respond to the slowly endpoint" in {

      HttpClientTest(app.port).get(s"/$prefix/tracing/slowly").getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(5 seconds)) {

        val span = testSpanReporter.nextSpan().value

        span.operationName shouldBe s"$prefix.tracing.slowly.get"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/slowly"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 200


        span.parentId shouldBe ""

        span.from.until(span.to, ChronoUnit.MILLIS) shouldBe >= (app.slowlyServiceDuration.toMillis)
        testSpanReporter.nextSpan() shouldBe empty
      }
    }

    it should "resume the incoming context and respond to the ok endpoint" in {
//      HttpClientTest(app.port).get(s"/$prefix/tracing/ok", IncomingContext.headersB3).getStatusLine.getStatusCode shouldBe 200

      eventually(timeout(3 seconds)) {

        val span = testSpanReporter().nextSpan().value

        span.operationName shouldBe s"$prefix.tracing.ok.get"
        span.kind shouldBe Span.Kind.Server
        span.metricTags.get(plain("component")) shouldBe "spring-server"
        span.metricTags.get(plain("http.method")) shouldBe "GET"
        span.metricTags.get(plain("http.url")) shouldBe s"/$prefix/tracing/ok"
        span.metricTags.get(plainLong("http.status_code")) shouldBe 200

        testSpanReporter.nextSpan() shouldBe empty
      }
    }
  }
}
