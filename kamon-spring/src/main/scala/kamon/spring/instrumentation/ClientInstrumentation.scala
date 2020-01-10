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

package kamon.spring.instrumentation

import kamon.spring.instrumentation.advisor.{InterceptingAsyncHttpAccessorAdvisor, InterceptingSyncHttpAccessorAdvisor}
import kanela.agent.api.instrumentation.InstrumentationBuilder

class ClientInstrumentation extends InstrumentationBuilder {

  onSubTypesOf("org.springframework.http.client.support.InterceptingHttpAccessor")
    .advise(isConstructor, classOf[InterceptingSyncHttpAccessorAdvisor])

  onSubTypesOf("org.springframework.http.client.support.InterceptingAsyncHttpAccessor")
    .advise(isConstructor, classOf[InterceptingAsyncHttpAccessorAdvisor])
}
