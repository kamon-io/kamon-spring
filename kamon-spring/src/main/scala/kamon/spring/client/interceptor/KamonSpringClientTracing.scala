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
