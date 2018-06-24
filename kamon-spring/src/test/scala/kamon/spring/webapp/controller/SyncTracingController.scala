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

import javax.servlet.http.HttpServletResponse
import kamon.spring.webapp.utils.ApiUtils
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
@RequestMapping(Array("/sync/tracing"))
private[spring]
class SyncTracingController extends ApiUtils {

  @RequestMapping(Array("/ok"))
  private[spring] def home = "Hello World!"

  @RequestMapping(Array("/not-found"))
  private[spring] def notFound(response: HttpServletResponse) = {
    ResponseEntity.notFound().build()
  }

  @RequestMapping(Array("/error"))
  private[spring] def error(): ResponseEntity[Unit] = {
    ResponseEntity.status(INTERNAL_SERVER_ERROR).build()
  }

  @RequestMapping(Array("/exception"))
  private[spring] def unhandledException(response: HttpServletResponse) = {
    throw new RuntimeException("Blowing up from internal servlet")
  }

  @RequestMapping(Array("/slowly"))
  private[spring] def slowly(response: HttpServletResponse) =
    withDelay(SyncTracingController.slowlyServiceDuration.toMillis) {
      "Slowly api"
    }
}

object SyncTracingController {
  import concurrent.duration._
  val slowlyServiceDuration: FiniteDuration = 1 seconds
}
