package co.bigdata.helanto.apiserver

import co.bigdata.helanto.apiserver.Headers.Headers
import co.bigdata.helanto.config.InputCfg
import co.bigdata.helanto.utils.StreamUtils
import co.bigdata.helanto.utils.StreamUtils.EMPTY_STREAM

import com.fasterxml.jackson.core.util.RequestPayload
import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import okhttp3.tls.{Certificates, HandshakeCertificates}

import java.io.{BufferedReader, InputStream, InputStreamReader, Reader}
import java.net.{HttpURLConnection, URL}
import java.security.cert.X509Certificate

/** A representation of the API server */
case class APIServer(
  host: String,
  port: String,
  protocol: String = "http",
  private val certs: HandshakeCertificates = APIServer.DEFAULT_CERTS.build(),
  private val bearerToken: Headers = Headers.NO_HEADERS) {
  override def toString: String = s"$protocol://$host:$port"
}

object APIServer {

  /** An [[co.bigdata.helanto.apiserver.APIServer APIServer]] builder.
    * @example
    * {{{
    *   val server = APIServer.Builder()
    *    .withHost("127.0.0.1")
    *    .withPort(80)
    *    .withProtocol("http")
    *    .withBearerToken("token")
    *    .withCert("""-----BEGIN CERTIFICATE----- ....""")
    *    .build
    * }}}
    */
  case class Builder(
    private val server: APIServer = DEFAULT_SERVER,
    private val certBuilder: HandshakeCertificates.Builder = DEFAULT_CERTS) {

    /** Adds host information to the APIServer
      * @example The following are valid input parameters for an APIServer host:
      *   - localhost
      *   - google.com
      *   - https://127.0.0.1:8080
      *
      * @param host can be just an IP, an FQDN, or a URL
      * @return a new instance of the builder
      */
    def withHost(host: String): Builder =
      host.replaceAll("/", "").split(":").toList match {
        case host :: Nil                     => this.copy(server = server.copy(host = host))
        case protocol :: host :: port :: Nil => withProtocol(protocol).withHost(host).withPort(port)
        case _                               => this
      }

    def withPort(port: Int): Builder = withPort(port.toString)

    def withPort(port: String): Builder = this.copy(server = server.copy(port = port))

    def withBearerToken(token: String): Builder =
      this.copy(server = server.copy(bearerToken = Headers.fromToken(token)))

    def withCert(cert: X509Certificate): Builder =
      this.copy(certBuilder = this.certBuilder.addTrustedCertificate(cert))

    def withCert(cert: String): Builder = withCert(Certificates.decodeCertificatePem(cert))

    def withProtocol(protocol: String): Builder =
      this.copy(server = server.copy(protocol = protocol))

    def build: APIServer = server.copy(certs = certBuilder.build())
  }

  /** The default API server when using `kubectl proxy --port=8080` */
  private lazy val DEFAULT_SERVER = APIServer("127.0.0.1", "8080", "http")

  /** A [[okhttp3.tls.HandshakeCertificates.Builder Builder]] with the platform certificates pre-installed */
  private def DEFAULT_CERTS = new HandshakeCertificates.Builder().addPlatformTrustedCertificates()

  /** Constructs an APIServer from an [[co.bigdata.helanto.config.InputCfg InputConfig]] */
  def apply(cfg: InputCfg): APIServer = {
    val builder = (cfg.token, cfg.cert) match {
      case (None, None)       => Builder().withHost(cfg.host)
      case (Some(t), None)    => Builder().withHost(cfg.host).withBearerToken(t)
      case (None, Some(c))    => Builder().withHost(cfg.host).withCert(c)
      case (Some(t), Some(c)) => Builder().withHost(cfg.host).withBearerToken(t).withCert(c)
    }

    builder.build
  }

  /** A representation of the different available endpoints */
  sealed trait Endpoint
  case class ConfigMapEndpoint(server: APIServer, namespace: String) extends Endpoint
  case class PersonWatchEndpoint(server: APIServer) extends Endpoint

  object Endpoint {

    def toURL(endpoint: Endpoint): URL = endpoint match {
      case ConfigMapEndpoint(server, ns) => new URL(s"$server/api/v1/namespaces/$ns/configmaps")
      case PersonWatchEndpoint(server)   =>
        // Watch for events in all namespaces. We need cluster-wide "watch" permissions.
        // If we want to "watch" a specific namespace, we could do the following:
        // s"$server/apis/extensions.helanto.co/v1/namespaces/$ns/persons?watch=true"
        new URL(s"$server/apis/extensions.helanto.co/v1/persons?watch=true")
    }
  }

  def streamFromServer(endpoint: Endpoint): InputStream = endpoint match {
    case ConfigMapEndpoint(server, ns) => EMPTY_STREAM
    case PersonWatchEndpoint(server)   =>
      val response = performGET(server)(Endpoint.toURL(endpoint))

      response match {
        case scala.util.Failure(ex)  => EMPTY_STREAM
        case scala.util.Success(res) => res.body().source().inputStream()
      }
  }

  def postToServer(endpoint: Endpoint)(payload: Array[Byte]): InputStream =
    endpoint match {
      case ConfigMapEndpoint(server, ns) =>
        performPostJSON(server)(Endpoint.toURL(endpoint))(payload) match {
          case scala.util.Failure(ex)  => EMPTY_STREAM
          case scala.util.Success(res) => res.body().source().inputStream()
        }
      case PersonWatchEndpoint(server)   => EMPTY_STREAM
    }

  private def performPostJSON(
    server: APIServer
  )(
    url: URL
  )(
    payload: Array[Byte]
  ): scala.util.Try[Response] = {
    val body = RequestBody.create(payload, JSON)
    performPost(server)(url)(body)
  }

  private def performPost(
    server: APIServer
  )(
    url: URL
  )(
    payload: RequestBody
  ): scala.util.Try[Response] = {
    val initBuilder = new Request.Builder().url(url)
    val builder =
      server.bearerToken.foldLeft(initBuilder) { (builder, header) =>
        StreamUtils.safePrintLine(s"Header: '${header._1}: ${header._2}'")
        builder.addHeader(header._1, header._2)
      }

    val request = builder.post(payload).build()
    scala.util.Try { client(server).newCall(request).execute() }
  }

  private def performGET(
    server: APIServer
  )(
    url: URL
  ): scala.util.Try[Response] = {
    val initBuilder = new Request.Builder().url(url)
    val builder =
      server.bearerToken.foldLeft(initBuilder) { (builder, header) =>
        StreamUtils.safePrintLine(s"Header: '${header._1}: ${header._2}'")
        builder.addHeader(header._1, header._2)
      }

    val request = builder.get().build()
    scala.util.Try { client(server).newCall(request).execute() }
  }

  private def clientBuilder = new OkHttpClient.Builder()
    .connectTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)

  private lazy val client: APIServer => OkHttpClient = {
    // A cache from APIServer => OkHttpClient ---> Avoid building new clients with the same config
    val clientsCache = scala.collection.mutable.Map.empty[APIServer, OkHttpClient]
    server =>
      clientsCache.getOrElseUpdate(
        server, {
          StreamUtils.safePrintLine(s"Building new HTTP client for $server")
          clientBuilder
            .sslSocketFactory(server.certs.sslSocketFactory(), server.certs.trustManager())
            .build()
        }
      )
  }

  private val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")

  /** Posts to API server using java.io and java.net core functions */
  private def legacyPostToServer(endpoint: Endpoint)(payload: Array[Byte]): InputStream =
    endpoint match {
      case ConfigMapEndpoint(server, ns) =>
        // TODO handle authentication using BEARER TOKEN.
        // TODO handle conflicting configmaps modifications - current implementation does not handle 409 response codes
        val connection: HttpURLConnection =
          Endpoint.toURL(endpoint).openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setFixedLengthStreamingMode(payload.size)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.connect()

        val os = connection.getOutputStream
        os.write(payload)
        os.flush()
        os.close()

        connection.getInputStream
      case PersonWatchEndpoint(server)   => EMPTY_STREAM
    }
}
