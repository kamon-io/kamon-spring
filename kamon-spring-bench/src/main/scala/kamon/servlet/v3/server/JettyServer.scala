/*
 * =========================================================================================
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

package kamon.servlet.v3.server

import java.net.InetSocketAddress
import java.util

import javax.servlet.{DispatcherType, Servlet}
import kamon.servlet.v3.KamonFilterV3
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

/**
  * Runs a Servlet or a Filter on an embedded Jetty server.
  */
class JettyServer(socketAddress: InetSocketAddress = new InetSocketAddress(0)) {
  val server = new Server(socketAddress)
  val context = new ServletContextHandler(server, "/")

  def start(): this.type = {
    val servlet = new ServletHolder(classOf[BenchServlet])

    servlet.setAsyncSupported(true)
    context.addServlet(servlet, "/")
    context.addFilter(classOf[KamonFilterV3], "/tracing/*", util.EnumSet.allOf(classOf[DispatcherType]))
    server.start()
    this
  }

  def stop(): this.type = {
    server.stop()
    this
  }

  def join(): this.type = {
    server.join()
    this
  }

  def selectedPort: Int = {
    server.getConnectors()(0).asInstanceOf[ServerConnector].getLocalPort
  }
}

trait JettySupport {

  val servlet: Servlet

  private var jetty = Option.empty[JettyServer]

  def startServer(): Unit = {
    jetty = Some(new JettyServer().start())
  }

  def stopServer(): Unit = {
    jetty.foreach(_.stop())
  }

  def port: Int = jetty.get.selectedPort
}