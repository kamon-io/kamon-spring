package kamon.spring.auto.client

import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.HttpClients

trait HttpClientSupport {

  def port: Int

  private val httpClient = HttpClients.createDefault()

  def get(path: String, headers: Seq[(String, String)] = Seq()): CloseableHttpResponse = {
    val request = new HttpGet(s"http://127.0.0.1:$port$path")
    headers.foreach { case (name, v) => request.addHeader(name, v) }
    val response = httpClient.execute(request)
    response.close()
    response
  }
}
