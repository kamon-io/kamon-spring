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

package kamon.spring.instrumentation.advisor

import kamon.spring.client.interceptor.{KamonAsyncInterceptorInjector, KamonSyncInterceptorInjector}
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.springframework.http.client.support.{InterceptingAsyncHttpAccessor, InterceptingHttpAccessor}

class InterceptingSyncHttpAccessorAdvisor
object InterceptingSyncHttpAccessorAdvisor {

  @Advice.OnMethodExit()
  def exitConstructor(@Advice.This accessor: InterceptingHttpAccessor): Unit = {
    KamonSyncInterceptorInjector.register(accessor)
  }
}

class InterceptingAsyncHttpAccessorAdvisor
object InterceptingAsyncHttpAccessorAdvisor {

  @Advice.OnMethodExit()
  def exitConstructor(@Advice.This accessor: InterceptingAsyncHttpAccessor): Unit = {
    KamonAsyncInterceptorInjector.register(accessor)
  }
}
