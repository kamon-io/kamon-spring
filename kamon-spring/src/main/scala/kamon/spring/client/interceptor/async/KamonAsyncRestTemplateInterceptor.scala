package kamon.spring.client.interceptor.async

import kamon.spring.client.interceptor.KamonSpringClientTracing
import org.springframework.http.HttpRequest
import org.springframework.http.client.{AsyncClientHttpRequestExecution, AsyncClientHttpRequestInterceptor, ClientHttpResponse}
import org.springframework.util.concurrent.{FailureCallback, ListenableFuture, SuccessCallback}

class KamonAsyncRestTemplateInterceptor extends AsyncClientHttpRequestInterceptor with KamonSpringClientTracing {

  override def intercept(request: HttpRequest, body: Array[Byte],
                         execution: AsyncClientHttpRequestExecution): ListenableFuture[ClientHttpResponse] = {
    val clientSpan = withNewSpan(request)
    val responseFuture = execution.executeAsync(request, body)

    responseFuture.addCallback(
      new SuccessCallback[ClientHttpResponse] {
        override def onSuccess(result: ClientHttpResponse): Unit = successContinuation(clientSpan)(result)
      },
      new FailureCallback {
        override def onFailure(ex: Throwable): Unit = failureContinuation(clientSpan)(ex)
      })

    responseFuture
  }
}
