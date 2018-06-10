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
