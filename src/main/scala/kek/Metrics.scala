package kek

import java.time.LocalDateTime

trait Metrics {
  def toVector: Vector[String]
}

final case class InstantMetrics(timestamp: LocalDateTime,
                                symbol: String,
                                meetingPoint: Double,
                                vwapAsks: Double,
                                vwapBids: Double,
                                vwapMidpoint: Double) extends Metrics {

  def toVector: Vector[String] = Vector(
    timestamp.toString,
    symbol,
    meetingPoint.toString,
    vwapAsks.toString,
    vwapBids.toString,
    vwapMidpoint.toString)

  override def toString: String = {

    val as = Vector(
      timestamp.toString,
      symbol,
      meetingPoint.toString,
      vwapAsks.toString,
      vwapBids.toString,
      vwapMidpoint.toString)

    val s = as.toString()

    s.slice(7,s.length-1)
  }

}

final case class SMA(startTimestamp: LocalDateTime,
                     endTimestamp: LocalDateTime,
                     symbol: String,
                     sma: Double) extends Metrics {
  def toVector: Vector[String] = Vector(
    startTimestamp.toString,
    endTimestamp.toString,
    symbol,
    sma.toString
  )
}

final case class EMA(startTimestamp: LocalDateTime,
                     endTimestamp: LocalDateTime,
                     symbol: String,
                     ema: Double) extends Metrics {
  def toVector: Vector[String] = Vector(
    startTimestamp.toString,
    endTimestamp.toString,
    symbol,
    ema.toString
  )
}

object Metrics {
  def instantFromEntry(entry: Entry, c: Long): InstantMetrics = {
    val midpoint = (entry.bids.last._1 + entry.asks.head._1) / 2
    val vwapAsks = getVwap(entry.asks, c)
    val vwapBids = getVwap(entry.bids, c)
    val vwapMidpoint = (vwapAsks + vwapBids) / 2

    InstantMetrics(entry.timestamp, entry.symbol, midpoint, vwapAsks, vwapBids, vwapMidpoint)
  }

  private def getVwap(bidsOrAsks: Vector[(Double, Double)], c: Long): Double = {
    val vwapNumerDenom: (Double, Double) = bidsOrAsks.foldLeft((0.0, 0.0))((numerDenom, bidOrAsk) => {
      if (numerDenom._2 + bidOrAsk._2 <= c) {
        (numerDenom._1 + bidOrAsk._2 * bidOrAsk._1, numerDenom._2 + bidOrAsk._2)
      } else {
        numerDenom
      }
    })
    vwapNumerDenom._1 / vwapNumerDenom._2
  }
}