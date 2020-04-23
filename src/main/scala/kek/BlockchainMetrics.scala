package kek

import java.time.LocalDateTime

import scala.collection.mutable
import scala.collection.mutable.{ArrayDeque, ListBuffer, Map}

final case class BlockchainMetrics(timestamp: LocalDateTime,
                                  feeQueue: ArrayDeque[Long],
                                  cryptoValQueue: ArrayDeque[Long],
                                  tsxQueue: ArrayDeque[Long],
                                  unsignedTsxQueue: ArrayDeque[Long],
                                  feeMetrics : Map[String,Double],
                                  cryptoValMetrics : Map[String,Double],
                                  tsxMetrics : Map[String,Double],
                                  unsignedTsxMetrics : Map[String,Double],
                                  lastBlockHeight : Int)
  extends Metrics {



  def toVector: Vector[String] = Vector(
    timestamp.toString,
    feeMetrics.toString.slice(7,feeMetrics.toString.length).replace("->","="),
    cryptoValMetrics.toString.slice(7,cryptoValMetrics.toString.length).replace("->","="),
    tsxMetrics.toString.slice(7,tsxMetrics.toString.length).replace("->","="),
    unsignedTsxMetrics.toString.slice(7,unsignedTsxMetrics.toString.length).replace("->","=")
  )

  override def toString: String = {

    val as = Vector(
      timestamp.toString,
      feeMetrics.toString.slice(7,feeMetrics.toString.length).replace("->","="),
      cryptoValMetrics.toString.slice(7,cryptoValMetrics.toString.length).replace("->","="),
      tsxMetrics.toString.slice(7,tsxMetrics.toString.length).replace("->","="),
      unsignedTsxMetrics.toString.slice(7,unsignedTsxMetrics.toString.length).replace("->","=")
    )

    val s = as.toString()

    s.slice(7,s.length-1)
  }

}

object BlockchainMetrics {

  def metricsInitialization(size: Int): BlockchainMetrics = {

    var defaultQueue = mutable.ArrayDeque[Long]()
    var defaultQueueTwo = mutable.ArrayDeque[Long]()
    var defaultQueueTree = mutable.ArrayDeque[Long]()
    var defaultQueueFour = mutable.ArrayDeque[Long]()
    val defaultMap = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val defaultMapTwo = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val defaultMapTree = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val defaultMapFour = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val lastBlockHeight = 0

    for (_ <- 1 to size) {
      defaultQueue += 0
      defaultQueueTwo += 0
      defaultQueueTree += 0
      defaultQueueFour += 0


    }

    val timestamp = LocalDateTime.now()

    BlockchainMetrics(timestamp, defaultQueue, defaultQueueTwo, defaultQueueTree, defaultQueueFour,
      defaultMap, defaultMapTwo, defaultMapTree, defaultMapFour, lastBlockHeight)


  }

  def updateMetrics(currentMetrics: BlockchainMetrics, newBlockHeights: List[Int]): BlockchainMetrics = {

    var currMet = currentMetrics

    for (height <- newBlockHeights) {

      val blockInfo = getBlockInfo(height)

      val fee = blockInfo("fee")
      val cryptoVal = blockInfo("cryptoVal")
      val tsxCount = blockInfo("tsxCount")
      val unsignedTsxCount = blockInfo("unsignedTsxCount")

      val (feeQueue, feeMetrics) = updateMetric(fee, currentMetrics.feeQueue, currentMetrics.feeMetrics)
      val (cryptoValQueue, cryptoValMetrics) = updateMetric(cryptoVal, currentMetrics.cryptoValQueue, currentMetrics.cryptoValMetrics)
      val (tsxQueue, tsxMetrics) = updateMetric(tsxCount, currentMetrics.tsxQueue, currentMetrics.tsxMetrics)
      val (unsignedTsxQueue, unsignedTsxMetrics) = updateMetric(unsignedTsxCount, currentMetrics.unsignedTsxQueue, currentMetrics.unsignedTsxMetrics)

      currMet = BlockchainMetrics(LocalDateTime.now(), feeQueue, cryptoValQueue, tsxQueue, unsignedTsxQueue, feeMetrics,
        cryptoValMetrics, tsxMetrics, unsignedTsxMetrics, height)
    }

    currMet
  }

  def updateMetric(newValue: Long, queue: ArrayDeque[Long], metrics: Map[String, Double]): (ArrayDeque[Long], Map[String, Double]) = {

    val popped = queue.removeHead()
    queue += newValue
    val newSum = metrics("sum") - popped + newValue
    metrics.update("sum", newSum)
    if (newValue > metrics("max")) metrics.update("max", newValue)
    if (newValue < metrics("min")) metrics.update("min", newValue)
    metrics.update("avg", queue.sum / queue.size)

    (queue, metrics)
  }

  def getUnsignedTSX(): Long = {

    val result = requests.get("https://blockchain.info/q/unconfirmedcount")
    val unsignedTSX = ujson.read(result.text).toString().toLong
    unsignedTSX

  }

  def getBlockInfo(blockHeight: Int): Map[String, Long] = {

    val url = "https://api.blockchair.com/bitcoin/blocks?q=id(".concat(blockHeight.toString).concat(")")
    val result = requests.get(url)
    val json = ujson.read(result.text)
    val data = json("data").arr(0)
    val fee = data("fee_total").toString().toLong
    val tsxCount = data("transaction_count").toString().toLong
    val cryptoVal = data("input_total").toString().toLong
    val unsignedTsxCount = getUnsignedTSX()

    val answer = scala.collection.mutable.Map("fee" -> fee, "tsxCount" -> tsxCount, "cryptoVal" -> cryptoVal, "unsignedTsxCount" -> unsignedTsxCount)

    answer
  }

  def getLastNHoursHeights(n: Int): List[Int] = {

    val url = "https://api.bitaps.com/btc/v1/blockchain/blocks/last/".concat(n.toString).concat("/hours")
    val result = requests.get(url)
    val json = ujson.read(result.text)
    val data = json("data").arr
    var heights = new mutable.ListBuffer[Int]
    for (element <- data) {
      heights += element("height").toString().toInt

    }
    heights.toList
  }
}