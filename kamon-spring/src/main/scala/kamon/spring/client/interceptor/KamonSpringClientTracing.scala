/* =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
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

package kamon.spring.client.interceptor

import java.util

import kamon.Kamon
import kamon.instrumentation.http.HttpClientInstrumentation.RequestHandler
import kamon.instrumentation.http.HttpMessage.RequestBuilder
import kamon.instrumentation.http.{HttpClientInstrumentation, HttpMessage}
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.{HttpRequest, HttpStatus}

import scala.collection.JavaConverters._

trait KamonSpringClientTracing {

  val instrumentation = HttpClientInstrumentation.from(Kamon.config(), "spring-rest-template-client")

  protected def withNewSpan(request: HttpRequest): HttpClientInstrumentation.RequestHandler[HttpRequest] = {
    val requestHandler: HttpClientInstrumentation.RequestHandler[HttpRequest] = instrumentation.createHandler(getRequestBuilder(request), Kamon.currentContext())
    requestHandler
//    val clientRequestSpan = currentContext.get(SpanCustomizer.ContextKey)
//      .customize(clientSpanBuilder)
//      .start()

//    val contextWithClientSpan = currentContext.withKey(Span.ContextKey, clientRequestSpan)

//    val headers = request.getHeaders
//    val textMap = Kamon.contextCodec().HttpHeaders.encode(contextWithClientSpan)
//    textMap.values.foreach { case (name, value) => headers.add(name, value) }
//    clientRequestSpan
  }

  protected def successContinuation(requestHandler: RequestHandler[HttpRequest])(response: ClientHttpResponse): ClientHttpResponse = {
    val statusCode = response.getStatusCode

    requestHandler.span.tag("http.status_code", statusCode.value())

    if (statusCode.is5xxServerError())
      requestHandler.span.fail(("error"))

    if (statusCode.value() == HttpStatus.NOT_FOUND.value())
      requestHandler.span.name("not-found")

    requestHandler.span.finish()
    response
  }

  protected def failureContinuation(requestHandler: RequestHandler[HttpRequest])(error: Throwable): Unit = {
    requestHandler.span.fail("error.object", error)
    requestHandler.span.finish()
  }


  def getRequestBuilder(request: HttpRequest): RequestBuilder[HttpRequest] = new HttpMessage.RequestBuilder[HttpRequest] {

    private val _headers: util.Map[String, String] = request.getHeaders.toSingleValueMap

    override def url: String =
      request.getURI.toString

    override def path: String =
      request.getURI.getPath

    override def method: String =
      request.getMethod.name

    override def host: String =
      request.getURI.getHost

    override def port: Int =
      request.getURI.getPort

    override def read(header: String): Option[String] =
      Option(request.getHeaders.toSingleValueMap.get(header))

    override def write(header: String, value: String): Unit =
      _headers.put(header, value)

    override def readAll(): Map[String, String] = {
      val builder = Map.newBuilder[String, String]
      request.getHeaders.toSingleValueMap.asScala.foreach{case (key, value) => builder += (key -> value)}
      builder.result()
    }

    override def build(): HttpRequest = {
      request.getHeaders.setAll(_headers)
      request
    }
  }
}
