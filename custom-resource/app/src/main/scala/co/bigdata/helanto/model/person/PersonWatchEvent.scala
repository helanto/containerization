package co.bigdata.helanto.model.person

case class PersonWatchEvent(`type`: String, `object`: PersonK8s)

object PersonWatchEvent {
  import com.fasterxml.jackson.databind.ObjectMapper
  import com.fasterxml.jackson.module.scala.DefaultScalaModule

  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  def apply(json: String): Option[PersonWatchEvent] =
    scala.util.Try {
      objectMapper.readValue[PersonWatchEvent](json, classOf[PersonWatchEvent])
    }.toOption
}
