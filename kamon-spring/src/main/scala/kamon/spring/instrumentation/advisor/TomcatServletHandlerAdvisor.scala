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

import kamon.servlet.v3.KamonFilterV3
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.apache.catalina.core.StandardContext
import org.apache.tomcat.util.descriptor.web.{FilterDef, FilterMap}


class TomcatServletHandlerAdvisor
object TomcatServletHandlerAdvisor {

  @Advice.OnMethodEnter()
  def enterFilterStart(@Advice.This context: Object): Unit = {
    val ctx = context.asInstanceOf[StandardContext]

    val filterDef = new FilterDef()
    filterDef.setFilterClass(classOf[KamonFilterV3].getName)
    filterDef.setFilterName(classOf[KamonFilterV3].getSimpleName)
    ctx.addFilterDef(filterDef)

    val filterMap = new FilterMap()
    filterMap.setFilterName(classOf[KamonFilterV3].getSimpleName)
    filterMap.addURLPattern("/*")
    filterMap.setDispatcher("REQUEST")
    ctx.addFilterMapBefore(filterMap)
  }

}