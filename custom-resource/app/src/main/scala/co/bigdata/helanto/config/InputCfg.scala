package co.bigdata.helanto.config

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.stream.Collectors

case class InputCfg(
  private val protocol: Option[String] = None,
  host: String = "",
  private val port: Option[Int] = None,
  token: Option[String] = None,
  private val tokenPath: Option[String] = None,
  cert: Option[String] = None,
  private val certPath: Option[String] = None)

object InputCfg {

  /** A regular expression matching hostnames */
  private val REGEX = """(http|https):\/\/[^:]+:[0-9]+"""

  val builder = scopt.OParser.builder[InputCfg]

  val parser = {
    import builder._
    scopt.OParser.sequence(
      programName("person-operator"),
      head("version", "0.0.1"),
      opt[String]("protocol")
        .action((i, c) => c.copy(protocol = Some(i)))
        .text("protocol to use; either http or https"),
      opt[String]("host")
        .action((i, c) => c.copy(host = i))
        .required()
        .text("the host name to use. Both `https://hostname:443` and `hostname` are supported"),
      opt[Int]("port")
        .action((i, c) => c.copy(port = Some(i)))
        .text("the port of API server"),
      opt[String]("token")
        .action((i, c) => c.copy(token = Some(i)))
        .text("the Bearer token to use to authenticate to API server"),
      opt[String]("token-path")
        .action((i, c) => c.copy(tokenPath = Some(i)))
        .text("path to Bearer token"),
      opt[String]("cert")
        .action((i, c) => c.copy(cert = Some(i)))
        .text("the CA root certificate used for TLS communication"),
      opt[String]("cert-path")
        .action((i, c) => c.copy(certPath = Some(i)))
        .text("path to CA root certificate")
    )
  }

  /** Parse CLI input to an optional [[co.bigdata.helanto.config.InputCfg InputCfg]] */
  def fromArgs(args: Array[String]): Option[InputCfg] =
    scopt.OParser
      .parse(parser, args, InputCfg())
      .map { cfg => cfg.copy(token = parseToken(cfg)) }
      .map { cfg => cfg.copy(cert = parseCert(cfg)) }
      .flatMap { cfg => parseHost(cfg).map { host => cfg.copy(host = host) } }

  /** Safely parses token out of config */
  private def parseToken(cfg: InputCfg): Option[String] = cfg.token match {
    case None            =>
      cfg.tokenPath match {
        case Some(path) =>
          Some(Files.lines(Paths.get(path)).collect(Collectors.joining(System.lineSeparator())))
        case None       => None
      }
    case token @ Some(_) => token
  }

  /** Safely parses CA root certificate out of config */
  private def parseCert(cfg: InputCfg): Option[String] = cfg.cert match {
    case None           =>
      cfg.certPath match {
        case Some(path) =>
          Some(Files.lines(Paths.get(path)).collect(Collectors.joining(System.lineSeparator())))
        case None       => None
      }
    case cert @ Some(_) => cert
  }

  /** Safely parses host out of config */
  private def parseHost(cfg: InputCfg): Option[String] = {
    val host = cfg.host
    if (host.matches(REGEX)) Some(host)
    else
      cfg.protocol match {
        case Some(protocol) =>
          cfg.port match {
            case Some(port) => Some(s"$protocol://$host:$port")
            case None       => None
          }
        case None           => None
      }
  }
}
