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
