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

import kamon.Kamon
import kamon.context.Context
import kamon.spring.utils.SpanReporter
import kamon.spring.webapp.AppSupport
import kamon.trace.Span.TagValue
import kamon.trace.{Span, SpanCustomizer}
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Inside, Matchers, OptionValues}
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.client._

import scala.concurrent.duration._
import scala.reflect.ClassTag

trait ClientProvider {
  def port: Int
  def GetRequest[T: ClassTag](url: String): ResponseEntity[T]

  def validateError[T <: HttpStatusCodeException: ClassTag](exception: Throwable, validatorF: Int => Unit): Unit
}

object ClientProvider {

  abstract class Sync extends ClientProvider with AppSupport {
    import Inside._

    protected lazy val app: ConfigurableApplicationContext = startJettyApp(kamonSpringWebEnabled = false)

    def restTemplate: RestTemplate

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

  abstract class Async extends ClientProvider with AppSupport {
    import Inside._

    protected lazy val app: ConfigurableApplicationContext = startJettyApp(kamonSpringWebEnabled = false)

    def asyncRestTemplate: AsyncRestTemplate

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

trait ClientBehaviors extends KamonSpringLogger { this: FlatSpec
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

    it should "propagate the current context and generate a span called not-found when request produce 404" in {

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

    it should "propagate the current context and generate a span with error when request produce 500" in {

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

    it should "propagate the current context and pickup a SpanCustomizer to create a new span" in {

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
