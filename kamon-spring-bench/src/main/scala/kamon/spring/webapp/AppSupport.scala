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
  private var app = Option.empty[ConfigurableApplicationContext]
  private var _port = Option.empty[Int]

  def startApp(kamonWebEnabled: Boolean = true, kamonClientEnabled: Boolean = true): Unit = {
    System.setProperty("server.port", 0.toString)
    System.setProperty("kamon.spring.web.enabled", kamonWebEnabled.toString)
    System.setProperty("kamon.spring.rest-template.enabled", kamonClientEnabled.toString)
    val configApp = SpringApplication.run(classOf[AppRunner])
    app = Some(configApp)
    _port = Option(configApp.getEnvironment.getProperty("local.server.port").toInt)
  }

  def stopApp(): Unit = app.foreach(_.close())

  def port: Int = _port.get
}
