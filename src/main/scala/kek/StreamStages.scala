package kek

import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString

object StreamStages {
  val formatter: Flow[Vector[String], ByteString, Any] = CsvFormatting.format()

  val instantHeader: Source[Vector[String], Any] = Source.single(Vector("timestamp", "symbol", "meeting_point", "vwap_asks", "vwap_bids", "vwap_midpoint"))
  val blockchainHeader : Source[Vector[String], Any] = Source.single(Vector("timestamp", "feeMetrics", "cryptoValMetrics", "tsxMetrics", "unsignedTsxMetrics", "lastBlockHeight"))
  val continuousHeader: Source[Vector[String], Any] = Source.single(Vector("start_timestamp", "end_timestamp", "symbol", "moving_average"))

  def filter(symbol: Option[String]): Flow[Entry, Entry, Any] = symbol match {
    case None => Flow[Entry]
    case Some(symbol) => Flow[Entry].filter(_.symbol == symbol)
  }

  def instantMetrics(c: Long): Flow[Entry, InstantMetrics, Any] = Flow[Entry].map { entry =>
    Metrics.instantFromEntry(entry, c)
  }
}
