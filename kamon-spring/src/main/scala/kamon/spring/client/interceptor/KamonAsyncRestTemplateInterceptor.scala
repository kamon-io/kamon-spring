package kamon.spring.client.interceptor

import org.springframework.http.HttpRequest
import org.springframework.http.client.{AsyncClientHttpRequestExecution, AsyncClientHttpRequestInterceptor, ClientHttpResponse}
import org.springframework.util.concurrent.{ListenableFuture, SuccessCallback}

class KamonAsyncRestTemplateInterceptor extends AsyncClientHttpRequestInterceptor with KamonSpringClientTracing {

  override def intercept(request: HttpRequest, body: Array[Byte],
                         execution: AsyncClientHttpRequestExecution): ListenableFuture[ClientHttpResponse] = {
    val clientSpan = withNewSpan(request)
    val responseFuture = execution.executeAsync(request, body)

    responseFuture.addCallback(new SuccessCallback[ClientHttpResponse] {
      override def onSuccess(result: ClientHttpResponse): Unit = successContinuation(clientSpan)(result)
    }, (ex: Throwable) => failureContinuation(clientSpan)(ex))

    responseFuture
  }
}
