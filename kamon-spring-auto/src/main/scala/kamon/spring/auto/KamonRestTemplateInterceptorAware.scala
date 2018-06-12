package kamon.spring.auto

import java.util

import kamon.spring.KamonSpringLogger
import kamon.spring.client.interceptor.{KamonAsyncRestTemplateInterceptor, KamonRestTemplateInterceptor}
import org.springframework.http.client.{AsyncClientHttpRequestInterceptor, ClientHttpRequestInterceptor}
import org.springframework.http.client.support.{InterceptingAsyncHttpAccessor, InterceptingHttpAccessor}

import scala.collection.mutable

trait KamonRestTemplateInterceptorAware extends KamonSpringLogger {
  import collection.JavaConverters._

  protected def registerKamonInterceptor(accessor: InterceptingHttpAccessor): Unit = {
    val interceptors: mutable.Seq[ClientHttpRequestInterceptor] = accessor.getInterceptors.asScala
    if (!kamonInterceptorLoaded(interceptors)) {
      logger.info(s"Adding ${classOf[KamonRestTemplateInterceptor].getSimpleName} to $accessor")
      val newInterceptorList = new util.ArrayList[ClientHttpRequestInterceptor](accessor.getInterceptors)
      newInterceptorList.add(new KamonRestTemplateInterceptor)
      accessor.setInterceptors(newInterceptorList)
    }

  }

  @inline private def kamonInterceptorLoaded(interceptors: mutable.Seq[ClientHttpRequestInterceptor]) = {
    interceptors.exists(_.isInstanceOf[KamonRestTemplateInterceptor])
  }
}

trait KamonAsyncRestTemplateInterceptorAware extends KamonSpringLogger {
  import collection.JavaConverters._

  protected def registerKamonAsyncInterceptor(accessor: InterceptingAsyncHttpAccessor): Unit = {
    val interceptors: mutable.Seq[AsyncClientHttpRequestInterceptor] = accessor.getInterceptors.asScala
    if (!kamonInterceptorLoaded(interceptors)) {
      logger.info(s"Adding ${classOf[KamonRestTemplateInterceptor].getSimpleName} to $accessor")
      val newInterceptorList = new util.ArrayList[AsyncClientHttpRequestInterceptor](accessor.getInterceptors)
      newInterceptorList.add(new KamonAsyncRestTemplateInterceptor)
      accessor.setInterceptors(newInterceptorList)
    }

  }

  @inline private def kamonInterceptorLoaded(interceptors: mutable.Seq[AsyncClientHttpRequestInterceptor]) = {
    interceptors.exists(_.isInstanceOf[KamonAsyncRestTemplateInterceptor])
  }
}
