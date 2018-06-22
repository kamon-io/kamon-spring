package kamon.spring.auto

import kamon.spring.client.interceptor.KamonSyncInterceptorInjector
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.web.client.RestTemplate

class KamonRestTemplateCustomizer extends RestTemplateCustomizer with KamonSyncInterceptorInjector {

  override def customize(restTemplate: RestTemplate): Unit = register(restTemplate)
}
