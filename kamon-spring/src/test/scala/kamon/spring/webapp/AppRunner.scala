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
