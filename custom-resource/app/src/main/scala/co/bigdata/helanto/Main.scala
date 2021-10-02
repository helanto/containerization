package co.bigdata.helanto

import co.bigdata.helanto.apiserver.{APIServer, Headers}
import co.bigdata.helanto.apiserver.APIServer.PersonWatchEndpoint
import co.bigdata.helanto.config.InputCfg
import co.bigdata.helanto.model.person.PersonWatchEvent
import co.bigdata.helanto.reconcile.Reconciler
import co.bigdata.helanto.reconcile.Reconciler.Action
import co.bigdata.helanto.utils.StreamUtils

import java.io.{BufferedReader, InputStream, InputStreamReader}

object Main {

  /** Entry point of the operator.
    *
    * @example
    * {{{
    * --protocol <value>    protocol to use; either http or https
    * --host <value>        the host name to use. Both `https://hostname:443` and `hostname` are supported
    * --port <value>        the port of API server
    * --token <value>       the Bearer token to use to authenticate to API server
    * --token-path <value>  path to Bearer token
    *  --cert <value>        the CA root certificate used for TLS communication
    *  --cert-path <value>   path to CA root certificate
    * }}}
    */
  def main(args: Array[String]): Unit = {
    val maybeServer = for {
      cfg    <- InputCfg.fromArgs(args)
      server <- Some(APIServer(cfg))
      _      <- StreamUtils.safePrintLine(s"API Server: $server")
    } yield server

    val server = maybeServer.getOrElse(
      throw new Exception("Cannot create an APIServer instance")
    )

    def unsafeEvaluateAction: Action => Option[Unit] = Reconciler.unsafeEvaluateAction(server)

    val watchEndpoint: APIServer.Endpoint = PersonWatchEndpoint(server)

    StreamUtils.readLinesFromStream(APIServer.streamFromServer(watchEndpoint)) { line =>
      for {
        _           <- StreamUtils.safePrintLine(s"Got event from server - Response: $line")
        personWatch <- PersonWatchEvent(line)
        action      <- Reconciler(personWatch)
        res         <- unsafeEvaluateAction(action)
      } yield res
    }
  }
}
