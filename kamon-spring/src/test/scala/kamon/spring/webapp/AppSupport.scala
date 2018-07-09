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
