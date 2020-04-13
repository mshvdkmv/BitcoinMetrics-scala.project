package kek

import java.time.LocalDateTime

import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString

object StreamStages {
  // No need to test this – not implemented by us
  val formatter: Flow[Vector[String], ByteString, Any] = CsvFormatting.format()

  // No need to test this also – too simple
  val instantHeader: Source[Vector[String], Any] = Source.single(Vector("timestamp", "symbol", "meeting_point", "vwap_asks", "vwap_bids", "vwap_midpoint"))
  val continuousHeader: Source[Vector[String], Any] = Source.single(Vector("start_timestamp", "end_timestamp", "symbol", "moving_average"))

  def filter(symbol: Option[String]): Flow[Entry, Entry, Any] = symbol match {
    case None => Flow[Entry]
    case Some(symbol) => Flow[Entry].filter(_.symbol == symbol)
  }

  def instantMetrics(c: Long): Flow[Entry, InstantMetrics, Any] = Flow[Entry].map { entry =>
    Metrics.instantFromEntry(entry, c)
  }

  val sma: Flow[Seq[InstantMetrics], SMA, Any] = Flow[Seq[InstantMetrics]].map { metrics =>
    val midpoints = metrics.collect { metric => metric.vwapMidpoint }
    val average = midpoints.foldLeft((0.0, 1)) ((acc, i) => ((acc._1 + (i - acc._1) / acc._2), acc._2 + 1))._1
    SMA(metrics.head.timestamp, metrics.last.timestamp, metrics.head.symbol, average)
  }

  val smaToEma: Flow[SMA, EMA, Any] = Flow[SMA].scan((LocalDateTime.now(), LocalDateTime.now(), "", 1.0, 0)) { (res, el) =>
    val counter     = (res._5 + 1)
    val alpha       = 2.0 / counter
    val ema = alpha * el.sma + (1 - alpha) * res._4
    (el.startTimestamp, el.endTimestamp, el.symbol, ema, counter)
  }.map {
    tup => EMA(tup._1, tup._2, tup._3, tup._4)
  }.drop(1)
}
