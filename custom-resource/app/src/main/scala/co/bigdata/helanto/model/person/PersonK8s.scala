package co.bigdata.helanto.model.person

import co.bigdata.helanto.model.Metadata

case class PersonK8s(
  apiVersion: String,
  kind: String,
  metadata: Metadata,
  spec: Person)
