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

package kamon.spring.auto.webapp.utils

trait ApiUtils {

  def withDelay[A](timeInMillis: Long)(thunk: => A): A = {
    if (timeInMillis > 0) Thread.sleep(timeInMillis)
    thunk
  }
}

object ApiUtils {
  val defaultDuration: Long = 1000 // millis
}
