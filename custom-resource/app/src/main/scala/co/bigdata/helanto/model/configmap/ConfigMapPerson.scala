package co.bigdata.helanto.model.configmap

import co.bigdata.helanto.model.person.PersonK8s

import java.nio.charset.StandardCharsets

object ConfigMapPerson {

  val configMapTemplate =
    scala.io.Source
      .fromResource("configmap-template.json")
      .getLines()
      .reduce(_ + _)
      .replaceAll("\\s", "")

  def fromPerson(person: PersonK8s): Array[Byte] = {
    val json =
      s"""{\\"first-name\\":\\"${person.spec.firstName}\\",\\"last-name\\":\\"${person.spec.lastName}\\",\\"age\\":${person.spec.age}}"""
    val payload = configMapTemplate
      .replace("[CFMAP_NAME]", person.metadata.name + "-cfmap")
      .replace("[PERSON_NAME]", person.metadata.name)
      .replace("[UID]", person.metadata.uid)
      .replace("[NAMESPACE]", person.metadata.namespace)
      .replace("[PAYLOAD]", json)
    payload.getBytes(StandardCharsets.UTF_8)
  }
}
