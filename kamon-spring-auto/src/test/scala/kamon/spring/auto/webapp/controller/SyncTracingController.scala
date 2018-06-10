package kamon.spring.auto.webapp.controller

import javax.servlet.http.HttpServletResponse
import kamon.spring.auto.webapp.utils
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
@RequestMapping(Array("/sync/tracing"))
private[spring]
class SyncTracingController extends utils.ApiUtils {

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
