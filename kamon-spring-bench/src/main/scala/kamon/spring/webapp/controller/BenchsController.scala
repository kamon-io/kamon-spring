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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
@RequestMapping(Array("/tracing"))
private[spring]
class BenchsController extends ApiUtils {

  @RequestMapping(Array("/ok"))
  private[spring] def ok = "Hello World!"

  @RequestMapping(Array("/ok/async"))
  private[spring] def okAsync: Callable[ResponseEntity[String]] = () => ResponseEntity.ok("Hello World!")

  @RequestMapping(Array("/exception"))
  private[spring] def unhandledException(response: HttpServletResponse) = {
    throw new RuntimeException("Blowing up from internal servlet")
  }

  @RequestMapping(Array("/exception/async"))
  private[spring] def unhandledExceptionAsync(response: HttpServletResponse): Callable[ResponseEntity[String]] =
    () => throw new RuntimeException("Blowing up from internal servlet")

  @RequestMapping(Array("/slowly"))
  private[spring] def slowly(response: HttpServletResponse) =
    withDelay(BenchsController.slowlyServiceDuration.toMillis) {
      "Slowly api"
    }

  @RequestMapping(Array("/slowly/async"))
  private[spring] def slowlyAsync(response: HttpServletResponse): Callable[ResponseEntity[String]] =
    () => withDelay(BenchsController.slowlyServiceDuration.toMillis) {
      ResponseEntity.ok("Slowly api")
    }
}

object BenchsController {
  import concurrent.duration._
  val slowlyServiceDuration: FiniteDuration = 1 seconds
}
