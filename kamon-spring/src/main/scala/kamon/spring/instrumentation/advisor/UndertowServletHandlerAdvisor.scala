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

import io.undertow.servlet.api.{DeploymentInfo, FilterInfo}
import javax.servlet.DispatcherType
import kamon.servlet.v3.KamonFilterV3
import kanela.agent.libs.net.bytebuddy.asm.Advice

import scala.collection.JavaConverters._


class UndertowServletHandlerAdvisor
object UndertowServletHandlerAdvisor {

  @Advice.OnMethodExit()
  def exitConstructor(@Advice.Argument(0) deploymentInfo: DeploymentInfo): Unit = {
    val filterName = "kamonFilter"
    if (!deploymentInfo.getFilters.containsKey(filterName)) {
      val filterInfo = new FilterInfo(filterName, classOf[KamonFilterV3])
      filterInfo.setAsyncSupported(true)
      deploymentInfo.addFilter(filterInfo)
    }
    if (!deploymentInfo.getFilterMappings.asScala.exists(_.getFilterName == filterName)) {
      deploymentInfo.addFilterUrlMapping(filterName, "/*", DispatcherType.REQUEST)
    }
  }
}
