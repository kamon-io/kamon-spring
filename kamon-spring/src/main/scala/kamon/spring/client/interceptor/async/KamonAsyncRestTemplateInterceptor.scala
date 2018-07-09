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
