package kamon.spring.webapp

import javax.servlet._
import javax.servlet.http.HttpServletRequest
import kamon.servlet.v3.KamonFilterV3
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@SpringBootApplication
class AppRunner {

  @Bean
  def kamonFilterRegistration(@Value("${webapp.kamon.enabled}") enabled: Boolean): FilterRegistrationBean[KamonFilterV3] = {
    val registrationBean = new FilterRegistrationBean[KamonFilterV3]
    registrationBean.setFilter(new KamonFilterV3)
    registrationBean.addUrlPatterns("/*")
    registrationBean.setEnabled(enabled)
    registrationBean.setName("kamonFilter")
    registrationBean.setAsyncSupported(true)
    registrationBean.setOrder(Int.MinValue)
    registrationBean
  }

//  @Bean
//  def debugFilterRegistration: FilterRegistrationBean[DebugFilter] = {
//    val registrationBean = new FilterRegistrationBean[DebugFilter]
//    registrationBean.setFilter(new DebugFilter)
//    registrationBean.addUrlPatterns("/*")
//    registrationBean.setEnabled(true)
//    registrationBean.setName("debugFilter")
//    registrationBean.setAsyncSupported(true)
//    registrationBean
//  }
}

@Component
class CustomizationBean extends WebServerFactoryCustomizer[ConfigurableServletWebServerFactory] {
  override def customize(server: ConfigurableServletWebServerFactory): Unit = {
    server.setPort(0) // Random port
  }
}

class DebugFilter extends Filter {
  override def init(filterConfig: FilterConfig): Unit = ()
  override def destroy(): Unit = ()

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    chain.doFilter(request, response)
    val req = request.asInstanceOf[HttpServletRequest]
    println(s"***** isAsyncStarted: ${req.isAsyncStarted}")
  }
}
