package kamon.spring.webapp

import java.util.concurrent.Callable

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import kamon.servlet.v3.KamonFilterV3
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation._

@SpringBootApplication
class AppRunner {

  @Bean
  def kamonFilterRegistration: FilterRegistrationBean[KamonFilterV3] = {
    val registrationBean = new FilterRegistrationBean[KamonFilterV3]
    registrationBean.setFilter(new KamonFilterV3)
    registrationBean.addUrlPatterns("/*")
    registrationBean.setEnabled(true)
    registrationBean.setName("kamonFilter")
    registrationBean.setAsyncSupported(true)
    registrationBean.setOrder(Int.MinValue)
    registrationBean
  }

  @Bean
  def debugFilterRegistration: FilterRegistrationBean[DebugFilter] = {
    val registrationBean = new FilterRegistrationBean[DebugFilter]
    registrationBean.setFilter(new DebugFilter)
    registrationBean.addUrlPatterns("/*")
    registrationBean.setEnabled(true)
    registrationBean.setName("debugFilter")
    registrationBean.setAsyncSupported(true)
    registrationBean
  }
}

@Component
class CustomizationBean extends WebServerFactoryCustomizer[ConfigurableServletWebServerFactory] {
  override def customize(server: ConfigurableServletWebServerFactory): Unit = {
    server.setPort(0) // Random port
  }
}

class DebugFilter extends Filter {
  override def init(filterConfig: FilterConfig): Unit = ()
  override def destroy(): Unit = ()

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    chain.doFilter(request, response)
    val req = request.asInstanceOf[HttpServletRequest]
    println(s"***** isAsyncStarted: ${req.isAsyncStarted}")
  }
}

@RestController
@RequestMapping(Array("/sync/tracing"))
private[spring]
class SyncTracing extends ApiUtils {

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

  @RequestMapping(Array("/unhandled-error"))
  private[spring] def unhandledError(response: HttpServletResponse) = {
    throw new RuntimeException("Forcing an exception!")
  }

  @RequestMapping(Array("/slowly"))
  private[spring] def slowly(response: HttpServletResponse) =
    withDelay(SyncTracing.slowlyServiceDuration.toMillis) {
      "Slowly api"
    }
}

object SyncTracing {
  import concurrent.duration._
  val slowlyServiceDuration: FiniteDuration = 1 seconds
}

@RestController
@RequestMapping(Array("/async/tracing"))
private[spring]
class AsyncTracing {

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

  @RequestMapping(Array("/unhandled-error"))
  private[spring] def unhandledRrror(response: HttpServletResponse): Callable[ResponseEntity[Unit]] = new Callable[ResponseEntity[Unit]] {
    override def call(): ResponseEntity[Unit] = throw new RuntimeException("Forcing an exception!")
  }
}

trait ApiUtils {

  def withDelay[A](timeInMillis: Long)(thunk: => A): A = {
    if (timeInMillis > 0) Thread.sleep(timeInMillis)
    thunk
  }
}

object ApiUtils {

  val defaultDuration: Long = 1000 // millis
}

trait AppSupport {
  private var app = Option.empty[ConfigurableApplicationContext]
  private var _port = Option.empty[Int]

  def startApp(): Unit = {
    val configApp = SpringApplication.run(classOf[AppRunner])
    app = Some(configApp)
    _port = Option(configApp.getEnvironment.getProperty("local.server.port").toInt)
  }

  def stopApp(): Unit = app.foreach(_.close())

  def port: Int = _port.get
}
