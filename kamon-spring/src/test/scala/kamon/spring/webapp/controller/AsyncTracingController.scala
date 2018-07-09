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

package kamon.spring.webapp.controller

import java.util.concurrent.Callable

import javax.servlet.http.HttpServletResponse
import kamon.spring.webapp.utils.ApiUtils
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
@RequestMapping(Array("/async/tracing"))
private[spring]
class AsyncTracingController extends ApiUtils {

  @RequestMapping(Array("/ok"))
  private[spring] def home: Callable[String] = new Callable[String] {
    override def call(): String = "Hello World!"
  }

  @RequestMapping(Array("/not-found"))
  private[spring] def notFound(response: HttpServletResponse): Callable[ResponseEntity[Unit]] = new Callable[ResponseEntity[Unit]] {
    override def call() = ResponseEntity.notFound().build()
  }

  @RequestMapping(Array("/error"))
  private[spring] def error(response: HttpServletResponse): Callable[ResponseEntity[Unit]] = new Callable[ResponseEntity[Unit]] {
    override def call(): ResponseEntity[Unit] = ResponseEntity.status(INTERNAL_SERVER_ERROR).build()
  }

  @RequestMapping(Array("/exception"))
  private[spring] def unhandledException(response: HttpServletResponse): Callable[ResponseEntity[Unit]] = new Callable[ResponseEntity[Unit]] {
    override def call(): ResponseEntity[Unit] = throw new RuntimeException("Blowing up from internal servlet")
  }

  @RequestMapping(Array("/slowly"))
  private[spring] def slowly(response: HttpServletResponse): Callable[ResponseEntity[String]] = new Callable[ResponseEntity[String]] {
    override def call(): ResponseEntity[String] =
      withDelay(AsyncTracingController.slowlyServiceDuration.toMillis) {
        ResponseEntity.ok("Slowly api")
      }
  }
}

object AsyncTracingController {
  import concurrent.duration._
  val slowlyServiceDuration: FiniteDuration = 1 seconds
}
