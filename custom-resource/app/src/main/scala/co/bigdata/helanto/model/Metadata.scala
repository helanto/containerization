package co.bigdata.helanto.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
case class Metadata(name: String, namespace: String, uid: String)
