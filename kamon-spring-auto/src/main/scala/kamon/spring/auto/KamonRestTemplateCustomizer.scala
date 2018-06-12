package kamon.spring.auto

import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.web.client.RestTemplate

class KamonRestTemplateCustomizer extends RestTemplateCustomizer with KamonRestTemplateInterceptorAware {

  override def customize(restTemplate: RestTemplate): Unit = registerKamonInterceptor(restTemplate)
}
