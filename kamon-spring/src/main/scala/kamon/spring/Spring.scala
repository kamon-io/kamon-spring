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

package kamon.spring

import com.typesafe.config.Config
import javax.servlet.http.HttpServletRequest
import kamon.util.DynamicAccess
import kamon.{Kamon, OnReconfigureHook}
import org.springframework.http.HttpRequest

object Spring {
  @volatile var nameGenerator: NameGenerator = nameGeneratorFromConfig(Kamon.config())
  @volatile var addHttpStatusCodeAsMetricTag: Boolean = addHttpStatusCodeAsMetricTagFromConfig(Kamon.config())

  def generateOperationName(request: HttpServletRequest): String = nameGenerator.generateOperationName(request)

  def generateHttpClientOperationName(request: HttpRequest): String = nameGenerator.generateHttpClientOperationName(request)

  private def nameGeneratorFromConfig(config: Config): NameGenerator = {
    val dynamic = new DynamicAccess(getClass.getClassLoader)
    val nameGeneratorFQCN = config.getString("kamon.spring.name-generator")
    dynamic.createInstanceFor[NameGenerator](nameGeneratorFQCN, Nil).get
  }

  private def addHttpStatusCodeAsMetricTagFromConfig(config: Config): Boolean =
    config.getBoolean("kamon.spring.add-http-status-code-as-metric-tag")


  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit = {
      nameGenerator = nameGeneratorFromConfig(newConfig)
      addHttpStatusCodeAsMetricTag = addHttpStatusCodeAsMetricTagFromConfig(newConfig)
    }
  })
}

trait NameGenerator {
  def generateOperationName(request: HttpServletRequest): String
  def generateHttpClientOperationName(request: HttpRequest): String
}

class DefaultNameGenerator extends NameGenerator {

  import java.util.Locale

  import scala.collection.concurrent.TrieMap

  private val localCache = TrieMap.empty[String, String]
  private val normalizePattern = """\$([^<]+)<[^>]+>""".r

  override def generateHttpClientOperationName(request: HttpRequest): String = {
    request.getURI.toASCIIString
  }

  override def generateOperationName(request: HttpServletRequest): String = {

    localCache.getOrElseUpdate(s"${request.getMethod}${request.getRequestURI}", {
      // Convert paths of form GET /foo/bar/$paramname<regexp>/blah to foo.bar.paramname.blah.get
      val uri = request.getRequestURI
      val p = normalizePattern.replaceAllIn(uri, "$1").replace('/', '.').dropWhile(_ == '.')
      val normalisedPath = {
        if (p.lastOption.exists(_ != '.')) s"$p."
        else p
      }
      s"$normalisedPath${request.getMethod.toLowerCase(Locale.ENGLISH)}"
    })
  }
}
