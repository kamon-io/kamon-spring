package kamon.spring

import org.slf4j.{Logger, LoggerFactory}

trait KamonSpringLogger {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
