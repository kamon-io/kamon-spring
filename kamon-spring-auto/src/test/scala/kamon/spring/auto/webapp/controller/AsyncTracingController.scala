package kamon.spring.auto.webapp.controller

import java.util.concurrent.Callable

import javax.servlet.http.HttpServletResponse
import kamon.spring.auto.webapp.utils
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
@RequestMapping(Array("/async/tracing"))
private[spring]
class AsyncTracingController extends utils.ApiUtils {

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
  private[spring] def slowly(response: HttpServletResponse) =
    withDelay(AsyncTracingController.slowlyServiceDuration.toMillis) {
      "Slowly api"
    }
}

object AsyncTracingController {
  import concurrent.duration._
  val slowlyServiceDuration: FiniteDuration = 1 seconds
}
