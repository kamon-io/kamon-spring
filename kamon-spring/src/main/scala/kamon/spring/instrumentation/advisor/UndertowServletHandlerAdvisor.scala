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
