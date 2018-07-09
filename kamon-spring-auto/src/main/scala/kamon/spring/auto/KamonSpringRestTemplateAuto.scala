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

import javax.annotation.PostConstruct
import kamon.spring.client.interceptor.{KamonAsyncInterceptorInjector, KamonSyncInterceptorInjector}
import org.springframework.boot.autoconfigure.condition._
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.http.client.support.{InterceptingAsyncHttpAccessor, InterceptingHttpAccessor}
import org.springframework.web.client.RestTemplate

@Configuration
@ConditionalOnClass(Array(classOf[RestTemplate]))
@ConditionalOnProperty(name = Array("kamon.spring.rest-template.enabled"), havingValue = "true", matchIfMissing = true)
class KamonSpringRestTemplateAuto {

  @Configuration
  @ConditionalOnBean(Array(classOf[InterceptingHttpAccessor]))
  class KamonRestTemplateTracingConfig(accessors: java.util.Set[InterceptingHttpAccessor]) extends KamonSyncInterceptorInjector {
    import collection.JavaConverters._

    @PostConstruct
    def init(): Unit = {
      logger.info("Initializing Kamon RestTemplate interceptors")
      accessors.asScala.foreach(register)
    }
  }

  @Configuration
  @ConditionalOnBean(Array(classOf[InterceptingAsyncHttpAccessor]))
  @ConditionalOnClass(Array(classOf[InterceptingAsyncHttpAccessor]))
  class KamonAsyncRestTemplateTracingConfig(accessors: java.util.Set[InterceptingAsyncHttpAccessor]) extends KamonAsyncInterceptorInjector {
    import collection.JavaConverters._

    @PostConstruct
    def init(): Unit = {
      logger.info("Initializing Kamon Async RestTemplate interceptors")
      accessors.asScala.foreach(register)
    }
  }

  @Configuration
  @ConditionalOnClass(Array(classOf[RestTemplateCustomizer]))
  class TracingRestTemplateCustomizerConfiguration {

    @Bean
    @ConditionalOnMissingBean(Array(classOf[KamonRestTemplateCustomizer]))
    def tracingRestTemplateCustomizer: KamonRestTemplateCustomizer = new KamonRestTemplateCustomizer
  }
}
