package kamon.spring.auto.webapp

import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

trait AppSupport {
  private var _app = Option.empty[ConfigurableApplicationContext]
  private var _port = Option.empty[Int]

  def startApp(kamonSpringWebEnabled: Boolean = true): ConfigurableApplicationContext = {
    System.setProperty("kamon.spring.web.enabled", kamonSpringWebEnabled.toString)
    val configApp = SpringApplication.run(classOf[AppRunner])
    _app = Some(configApp)
    _port = Option(configApp.getEnvironment.getProperty("local.server.port").toInt)
    configApp
  }

  def stopApp(): Unit = _app.foreach(_.close())

  def port: Int = _port.get
}
