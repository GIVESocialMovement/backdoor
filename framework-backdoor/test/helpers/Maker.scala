package helpers

import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse

object Maker {
  def wsResponse(statusValue: Int, jsonValue: JsValue): WSResponse = {
    new WSResponse {
      override def body = ???

      override def bodyAsBytes = ???

      override def cookies = ???

      override def xml = ???

      override def json = jsonValue

      override def headers = ???

      override def cookie(name: String) = ???

      override def underlying[T] = ???

      override def bodyAsSource = ???

      override def allHeaders = ???

      override def statusText = ???

      override def status = statusValue
    }
  }

}
