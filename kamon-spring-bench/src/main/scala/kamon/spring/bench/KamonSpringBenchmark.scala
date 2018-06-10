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

package kamon.spring.bench

import java.util.concurrent.TimeUnit

import kamon.spring.client.HttpClientSupport
import kamon.spring.webapp.AppSupport
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
class KamonFilterBenchmark extends HttpClientSupport with AppSupport {

  @Setup(Level.Trial)
  def setup(): Unit = {
    startApp()
  }

  @TearDown(Level.Trial)
  def doTearDown(): Unit = {
    stopApp()
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
    * This benchmark attempts to measure the performance with tracing and metrics enabled using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_new_span_async(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok/async"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled when the
    * request finished with an unhandled exception.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_new_span_on_exc(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/exception"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled when the
    * request finished with an unhandled exception using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_new_span_on_exc_async(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/exception/async"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing enabled and metrics disabled.
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
    * This benchmark attempts to measure the performance with tracing enabled and metrics disabled
    * using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.servlet.metrics.enabled=false"))
  def trace_new_span_no_metrics_async(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok/async"))
  }

  /**
    * This benchmark attempts to measure the performance with tracing and metrics enabled when the request
    * comes with an existent context.
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
    * This benchmark attempts to measure the performance with tracing and metrics enabled when the request
    * comes with an existent context using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork
  def trace_resuming_span_async(blackhole: Blackhole, incomingContext: IncomingContext): Unit = {
    blackhole.consume(get("/tracing/ok/async"), incomingContext.headersB3)
  }

  /**
    * This benchmark attempts to measure the performance with tracing enabled and metrics disabled
    * when the request comes with an existent context.
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
    * This benchmark attempts to measure the performance with tracing enabled and metrics disabled
    * when the request comes with an existent context using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.servlet.metrics.enabled=false"))
  def trace_resuming_span_no_metrics_async(blackhole: Blackhole, incomingContext: IncomingContext): Unit = {
    blackhole.consume(get("/tracing/ok/async"), incomingContext.headersB3)
  }

  /**
    * This benchmark attempts to measure the performance with no tracing neither metrics enabled.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.spring.web.enabled=false"))
  def none_tracing(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok"))
  }

  /**
    * This benchmark attempts to measure the performance with no tracing neither metrics enabled
    * using async servlet.
    *
    * @param blackhole a { @link Blackhole} object supplied by JMH
    */
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  @Fork(jvmArgsAppend = Array("-Dkamon.spring.web.enabled=false"))
  def none_tracing_async(blackhole: Blackhole): Unit = {
    blackhole.consume(get("/tracing/ok/async"))
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
