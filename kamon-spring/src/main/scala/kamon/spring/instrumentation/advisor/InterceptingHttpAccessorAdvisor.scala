package kamon.spring.instrumentation.advisor

import kamon.spring.client.interceptor.{KamonAsyncInterceptorInjector, KamonSyncInterceptorInjector}
import kanela.agent.libs.net.bytebuddy.asm.Advice
import org.springframework.http.client.support.{InterceptingAsyncHttpAccessor, InterceptingHttpAccessor}


class InterceptingSyncHttpAccessorAdvisor
object InterceptingSyncHttpAccessorAdvisor {

  @Advice.OnMethodExit()
  def exitConstructor(@Advice.This accessor: InterceptingHttpAccessor): Unit = {
    KamonSyncInterceptorInjector.register(accessor)
  }
}

class InterceptingAsyncHttpAccessorAdvisor
object InterceptingAsyncHttpAccessorAdvisor {

  @Advice.OnMethodExit()
  def exitConstructor(@Advice.This accessor: InterceptingAsyncHttpAccessor): Unit = {
    KamonAsyncInterceptorInjector.register(accessor)
  }
}
