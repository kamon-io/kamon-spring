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

package kamon.spring.auto

import java.util

import javax.servlet.DispatcherType
import kamon.servlet.v3.KamonFilterV3
import kamon.spring.KamonSpringLogger
import org.springframework.boot.autoconfigure.condition.{ConditionalOnProperty, ConditionalOnWebApplication}
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.{Bean, Configuration}


@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = Array("kamon.spring.web.enabled"), havingValue = "true", matchIfMissing = true)
class KamonSpringAuto extends KamonSpringLogger{

  @Bean
  def kamonFilterRegistration: FilterRegistrationBean = {
    val registrationBean = new FilterRegistrationBean()
    registrationBean.setFilter(new KamonFilterV3)
    registrationBean.addUrlPatterns("/*")
    registrationBean.setEnabled(true)
    registrationBean.setName("kamonFilter")
    registrationBean.setDispatcherTypes(util.EnumSet.of(DispatcherType.REQUEST))
    registrationBean.setOrder(Int.MinValue)
    registrationBean
  }

}
