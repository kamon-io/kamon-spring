package kamon.spring.webapp

import java.util

import javax.servlet.DispatcherType
import kamon.servlet.v3.KamonFilterV3
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.{Bean, Configuration}

@SpringBootApplication
class AppJettyRunner

@SpringBootApplication
class AppTomcatRunner

@SpringBootApplication
class AppUndertowRunner

@Configuration
@ConditionalOnProperty(name = Array("kamon.spring.servlet-container"), havingValue = "jetty", matchIfMissing = true)
class configJetty {

  @Bean
  def jettyEmbeddedServletContainerFactory = new JettyEmbeddedServletContainerFactory
}

@Configuration
@ConditionalOnProperty(name = Array("kamon.spring.servlet-container"), havingValue = "tomcat", matchIfMissing = false)
class configTomcat {

  @Bean
  def tomcatEmbeddedServletContainerFactory = new TomcatEmbeddedServletContainerFactory
}

@Configuration
@ConditionalOnProperty(name = Array("kamon.spring.servlet-container"), havingValue = "undertow", matchIfMissing = false)
class configUndertow {

  @Bean
  def undertowEmbeddedServletContainerFactory = new UndertowEmbeddedServletContainerFactory
}

@Configuration
class Config {

  @Bean
  def kamonFilterRegistration(@Value("${kamon.spring.web.enabled}") enabled: Boolean): FilterRegistrationBean = {
    val registrationBean = new FilterRegistrationBean()
    registrationBean.setFilter(new KamonFilterV3)
    registrationBean.addUrlPatterns("/*")
    registrationBean.setEnabled(enabled)
    registrationBean.setName("kamonFilter")
    registrationBean.setDispatcherTypes(util.EnumSet.of(DispatcherType.REQUEST))
    registrationBean.setOrder(Int.MaxValue)
    registrationBean
  }
}
