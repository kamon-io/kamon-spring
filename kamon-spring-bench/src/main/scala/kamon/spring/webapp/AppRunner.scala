package kamon.spring.webapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.stereotype.Component

@SpringBootApplication
class AppRunner

@Component
class CustomizationBean extends WebServerFactoryCustomizer[ConfigurableServletWebServerFactory] {
  override def customize(server: ConfigurableServletWebServerFactory): Unit = {
    server.setPort(0) // Random port
  }
}