package kamon.spring.webapp

import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

trait AppSupport {
  private var _app = Option.empty[ConfigurableApplicationContext]
  private var _port = Option.empty[Int]

  def startJettyApp(kamonSpringWebEnabled: Boolean = true): ConfigurableApplicationContext = {
    System.setProperty("kamon.spring.servlet-container", "jetty")
    startApp(kamonSpringWebEnabled, classOf[AppJettyRunner])
  }

  def startTomcatApp(kamonSpringWebEnabled: Boolean = true): ConfigurableApplicationContext = {
    System.setProperty("kamon.spring.servlet-container", "tomcat")
    startApp(kamonSpringWebEnabled, classOf[AppTomcatRunner])
  }

  def startUndertowApp(kamonSpringWebEnabled: Boolean = true): ConfigurableApplicationContext = {
    System.setProperty("kamon.spring.servlet-container", "undertow")
    startApp(kamonSpringWebEnabled, classOf[AppUndertowRunner])
  }

  private def startApp(kamonSpringWebEnabled: Boolean = true, appClass: Class[_]): ConfigurableApplicationContext = {
    System.setProperty("server.port", 0.toString)
    System.setProperty("kamon.spring.web.enabled", kamonSpringWebEnabled.toString)
    val configApp = SpringApplication.run(appClass)
    _app = Some(configApp)
    _port = Option(configApp.getEnvironment.getProperty("local.server.port").toInt)
    configApp
  }

  def stopApp(): Unit = _app.foreach(_.close())

  def port: Int = _port.get
}
