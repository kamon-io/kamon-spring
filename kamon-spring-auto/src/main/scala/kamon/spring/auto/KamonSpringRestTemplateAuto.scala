package kamon.spring.auto

import javax.annotation.PostConstruct
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
  class KamonRestTemplateTracingConfig(accessors: java.util.Set[InterceptingHttpAccessor]) extends KamonRestTemplateInterceptorAware {
    import collection.JavaConverters._

    @PostConstruct
    def init(): Unit = {
      logger.info("Initializing Kamon RestTemplate interceptors")
      accessors.asScala.foreach(registerKamonInterceptor)
    }
  }

  @Configuration
  @ConditionalOnBean(Array(classOf[InterceptingAsyncHttpAccessor]))
  @ConditionalOnClass(Array(classOf[InterceptingAsyncHttpAccessor]))
  class KamonAsyncRestTemplateTracingConfig(accessors: java.util.Set[InterceptingAsyncHttpAccessor]) extends KamonAsyncRestTemplateInterceptorAware {
    import collection.JavaConverters._

    @PostConstruct
    def init(): Unit = {
      logger.info("Initializing Kamon Async RestTemplate interceptors")
      accessors.asScala.foreach(registerKamonAsyncInterceptor)
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
