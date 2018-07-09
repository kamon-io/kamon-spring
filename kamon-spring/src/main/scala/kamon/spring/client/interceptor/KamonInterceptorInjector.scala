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
