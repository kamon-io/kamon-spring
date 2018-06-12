package kamon.spring.auto.webapp

import org.springframework.boot.autoconfigure.{EnableAutoConfiguration, SpringBootApplication}
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component
import org.springframework.web.client.{AsyncRestTemplate, RestTemplate}

@SpringBootApplication
class AppRunner

@Component
class CustomizationBean extends WebServerFactoryCustomizer[ConfigurableServletWebServerFactory] {
  override def customize(server: ConfigurableServletWebServerFactory): Unit = {
    server.setPort(0) // Random port
  }
}

@Configuration
@EnableAutoConfiguration
class SpringWithKamonConfig {

  @Bean
  def restTemplateDefault: RestTemplate = new RestTemplate

  @Bean
  def asyncRestTemplateDefault: AsyncRestTemplate = new AsyncRestTemplate

  @Bean
  def restTemplateByBuilder(builder: RestTemplateBuilder): RestTemplate = builder.build
}
