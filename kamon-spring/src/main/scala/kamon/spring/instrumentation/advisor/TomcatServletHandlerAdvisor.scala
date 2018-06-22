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