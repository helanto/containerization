package co.bigdata.helanto.apiserver

object Headers {
  lazy val NO_HEADERS = Map.empty[String, String]

  type Headers = Map[String, String]

  def fromToken(token: String): Headers = Map("Authorization" -> s"Bearer $token")
}
