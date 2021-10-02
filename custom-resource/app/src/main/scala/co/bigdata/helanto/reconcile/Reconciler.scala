package co.bigdata.helanto.reconcile

import co.bigdata.helanto.apiserver.APIServer
import co.bigdata.helanto.apiserver.APIServer.ConfigMapEndpoint
import co.bigdata.helanto.model.configmap.ConfigMapPerson
import co.bigdata.helanto.model.person.PersonWatchEvent
import co.bigdata.helanto.utils.StreamUtils

object Reconciler {

  /** Describes the action that needs to be taken as part of a call to the reconcile function */
  sealed trait Action

  /** Post a config map to the API server */
  case class PostConfigMapAction(ns: String, payload: Array[Byte]) extends Action

  /** Ignore call to reconcile */
  case object IgnoreAction extends Action

  /** Returns the action to do next, given an input `watch` event */
  def apply(event: PersonWatchEvent): Option[Action] = event.`type` match {
    case "ADDED"   =>
      Some(
        PostConfigMapAction(
          event.`object`.metadata.namespace,
          ConfigMapPerson.fromPerson(event.`object`)
        )
      )
    case "DELETED" => Some(IgnoreAction)
    case _         => None
  }

  /** Unsafe interpret / execute an action */
  def unsafeEvaluateAction(server: APIServer)(action: Action): Option[Unit] =
    action match {
      case IgnoreAction                     => Some(())
      case PostConfigMapAction(ns, payload) =>
        val endpoint = ConfigMapEndpoint(server, ns)
        Some(
          StreamUtils.readLinesFromStream(APIServer.postToServer(endpoint)(payload)) { line =>
            StreamUtils.safePrintLine(s"Posted ConfigMap to server - Response: $line")
          }
        )
    }
}
