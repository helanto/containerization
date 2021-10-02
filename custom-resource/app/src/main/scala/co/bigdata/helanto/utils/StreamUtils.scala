package co.bigdata.helanto.utils

import java.io.{BufferedReader, InputStream, InputStreamReader}

object StreamUtils {

  /* An [[java.io.InputStream InputStream]] without any elements */
  val EMPTY_STREAM: InputStream = new InputStream { override def read(): Int = -1 }

  /** Reads lines from the input [[java.io.BufferedReader BufferedReader]] and applies the
    * provided function to each line.
    *
    * Completes when the function returns [[scala.None None]] or the reader finishes.
    * @todo aggregate return values `Ts` and return the aggregated value
    * @param reader input reader
    * @param f a function from the next line to an optional `T`
    * @tparam T the result data type.
    */
  def readLinesFromReader[T](reader: BufferedReader)(f: String => Option[T]): Unit = {
    val _ = for {
      nextLine  <- Option(reader.readLine()) // If Reader is done, returns None!
      t         <- f(nextLine)
      readLines <- Some(readLinesFromReader[T](reader)(f)) // Read next line
    } yield ()

    if (reader != null) reader.close() // close reader and release underlying resources
    ()
  }

  /** Reads lines from the provided [[java.io.InputStream InputStream]] and applies the function to each line */
  def readLinesFromStream[T](stream: InputStream)(f: String => Option[T]): Unit =
    readLinesFromReader(new BufferedReader(new InputStreamReader(stream)))(f)

  def safePrintLine(text: String): Option[Unit] = Some(println(text))
}
