/*
 * =========================================================================================
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

package kamon.servlet.v3.bench

import java.util.concurrent.TimeUnit

import kamon.Kamon
import kamon.servlet.v3.server.JettyServer
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.HttpClients
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
class KamonFilterBenchmark {

  val server = new JettyServer()
  var port: Int = 0

  @Setup(Level.Trial)
  def setup(): Unit = {
    Kamon.config()
    server.start()
    port = server.selectedPort
  }

  @TearDown(Level.Trial)
  def doTearDown(): Unit = {
    server.stop()
  }
  private val httpClient = HttpClients.createDefault()

  private def get(path: String, headers: Seq[(String, String)] = Seq()): CloseableHttpResponse = {
    val request = new HttpGet(s"http://127.0.0.1:$port$path")
    headers.foreach { case (name, v) => request.addHeader(name, v) }
    httpClient.execute(request)
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_new_span(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.servlet.metrics.enabled=false"))
  def trace_new_span_no_metrics(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_resuming_span(blackhole: Blackhole, incomingContext: IncomingContext): Unit = {
    blackhole.consume(get("/tracing/ok"), incomingContext.headersB3)
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.servlet.metrics.enabled=false"))
  def trace_resuming_span_no_metrics(blackhole: Blackhole, incomingContext: IncomingContext): Unit = {
    blackhole.consume(get("/tracing/ok"), incomingContext.headersB3)
  }

  /**
    * This benchmark attempts to measure the performance with NO tracing NEITHER metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def none_tracing(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/ok"))
  }

}

@State(Scope.Benchmark)
class IncomingContext {
  import kamon.trace.SpanCodec.B3.{Headers => B3Headers}

  val headersB3 = Seq(
    (B3Headers.TraceIdentifier, "1234"),
    (B3Headers.ParentSpanIdentifier, "2222"),
    (B3Headers.SpanIdentifier, "4321"),
    (B3Headers.Sampled, "1"),
    (B3Headers.Flags, "some=baggage;more=baggage"))
}
