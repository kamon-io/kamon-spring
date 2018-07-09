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

package kamon.spring.client.interceptor

import kamon.Kamon
import kamon.spring.Spring
import kamon.trace.{Span, SpanCustomizer}
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.{HttpRequest, HttpStatus}

trait KamonSpringClientTracing {

  protected def withNewSpan(request: HttpRequest): Span = {
    val currentContext = Kamon.currentContext()
    val parentSpan = currentContext.get(Span.ContextKey)

    val clientSpanBuilder = Kamon.buildSpan(Spring.generateHttpClientOperationName(request))
      .asChildOf(parentSpan)
      .withMetricTag("span.kind", "client")
      .withMetricTag("component", "spring-rest-template-client")
      .withMetricTag("http.method", request.getMethod.name())
      .withTag("http.url", request.getURI.toASCIIString)

    val clientRequestSpan = currentContext.get(SpanCustomizer.ContextKey)
      .customize(clientSpanBuilder)
      .start()

    val contextWithClientSpan = currentContext.withKey(Span.ContextKey, clientRequestSpan)

    val headers = request.getHeaders
    val textMap = Kamon.contextCodec().HttpHeaders.encode(contextWithClientSpan)
    textMap.values.foreach { case (name, value) => headers.add(name, value) }
    clientRequestSpan
  }

  protected def successContinuation(clientRequestSpan: Span)(response: ClientHttpResponse): ClientHttpResponse = {
    val statusCode = response.getStatusCode

    clientRequestSpan.tag("http.status_code", statusCode.value())

    if (statusCode.is5xxServerError())
      clientRequestSpan.addError("error")

    if (statusCode.value() == HttpStatus.NOT_FOUND.value())
      clientRequestSpan.setOperationName("not-found")

    clientRequestSpan.finish()
    response
  }

  protected def failureContinuation(clientRequestSpan: Span)(error: Throwable): Unit = {
    clientRequestSpan.addError("error.object", error)
    clientRequestSpan.finish()
  }

}
