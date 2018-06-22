package kamon.spring.client.interceptor

import java.util

import kamon.spring.KamonSpringLogger
import kamon.spring.client.interceptor.async.KamonAsyncRestTemplateInterceptor
import kamon.spring.client.interceptor.sync.KamonRestTemplateInterceptor
import org.springframework.http.client.support.{InterceptingAsyncHttpAccessor, InterceptingHttpAccessor}
import org.springframework.http.client.{AsyncClientHttpRequestInterceptor, ClientHttpRequestInterceptor}

import scala.collection.mutable

trait KamonSyncInterceptorInjector extends KamonSpringLogger {
  import collection.JavaConverters._

  def register(accessor: InterceptingHttpAccessor): Unit = {
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

object KamonSyncInterceptorInjector extends KamonSyncInterceptorInjector

trait KamonAsyncInterceptorInjector extends KamonSpringLogger {
  import collection.JavaConverters._

  def register(accessor: InterceptingAsyncHttpAccessor): Unit = {
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

object KamonAsyncInterceptorInjector extends KamonAsyncInterceptorInjector
