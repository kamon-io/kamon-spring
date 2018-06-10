package kamon.spring.client.interceptor

import kamon.trace.Span
import org.springframework.http.HttpRequest
import org.springframework.http.client.{ClientHttpRequestExecution, ClientHttpRequestInterceptor, ClientHttpResponse}

class KamonRestTemplateInterceptor extends ClientHttpRequestInterceptor with KamonSpringClientTracing {

  override def intercept(request: HttpRequest, body: Array[Byte],
                         execution: ClientHttpRequestExecution): ClientHttpResponse = {

    val clientRequestSpan: Span = withNewSpan(request)

    try {
      val response = execution.execute(request, body)
      successContinuation(clientRequestSpan)(response)
    } catch {
      case error: Throwable =>
        failureContinuation(clientRequestSpan)(error)
        throw error
    }
  }
}
