package kek

import java.time.LocalDateTime

import scala.collection.mutable



final case class BlockchainMetrics(timestamp: LocalDateTime,
                                   feeQueue: mutable.ArrayBuffer[Long],
                                   cryptoValQueue: mutable.ArrayBuffer[Long],
                                   tsxQueue: mutable.ArrayBuffer[Long],
                                   unsignedTsxQueue: mutable.ArrayBuffer[Long],
                                   feeMetrics : mutable.Map[String,Double],
                                   cryptoValMetrics : mutable.Map[String,Double],
                                   tsxMetrics : mutable.Map[String,Double],
                                   unsignedTsxMetrics : mutable.Map[String,Double],
                                   lastBlockHeight : Int)
  extends Metrics {



  def toVector: Vector[String] = Vector(
    timestamp.toString,
    feeMetrics.toString.slice(7,feeMetrics.toString.length).replace("->","="),
    cryptoValMetrics.toString.slice(7,cryptoValMetrics.toString.length).replace("->","="),
    tsxMetrics.toString.slice(7,tsxMetrics.toString.length).replace("->","="),
    unsignedTsxMetrics.toString.slice(7,unsignedTsxMetrics.toString.length).replace("->","="),
    lastBlockHeight.toString
  )

  override def toString: String = {

    val as = Vector(
      timestamp.toString,
      feeMetrics.toString.slice(7,feeMetrics.toString.length).replace("->","="),
      cryptoValMetrics.toString.slice(7,cryptoValMetrics.toString.length).replace("->","="),
      tsxMetrics.toString.slice(7,tsxMetrics.toString.length).replace("->","="),
      unsignedTsxMetrics.toString.slice(7,unsignedTsxMetrics.toString.length).replace("->","="),
      lastBlockHeight.toString
    )

    val s = as.toString()

    s.slice(7,s.length-1)
  }

}

object BlockchainMetrics {

  def metricsInitialization(size: Int): BlockchainMetrics = {


    val feeQueue = mutable.ArrayBuffer[Long]()
    val cryptoValQueue = mutable.ArrayBuffer[Long]()
    val tsxQueue = mutable.ArrayBuffer[Long]()
    val unsignedTsxQueue = mutable.ArrayBuffer[Long]()
    val feeMetrics = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val cryptoValMetrics = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val tsxMetrics = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val unsignedTsxMetrics = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)
    val lastBlockHeight = 0

    for (_ <- 1 to size) {
      feeQueue += 0
      cryptoValQueue += 0
      tsxQueue += 0
      unsignedTsxQueue += 0

    }

    val timestamp = LocalDateTime.now()

    BlockchainMetrics(timestamp, feeQueue, cryptoValQueue, tsxQueue, unsignedTsxQueue,
      feeMetrics, cryptoValMetrics, tsxMetrics, unsignedTsxMetrics, lastBlockHeight)


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

  def updateMetric(newValue: Long, queue: mutable.ArrayBuffer[Long], metrics: mutable.Map[String, Double]): (mutable.ArrayBuffer[Long], mutable.Map[String, Double]) = {

    val popped = queue(0)
    queue.trimStart(1)
    queue += newValue
    val newSum = metrics("sum") - popped + newValue
    metrics.update("sum", newSum)
    if (newValue > metrics("max")) metrics.update("max", newValue)
    if (newValue < metrics("min")) metrics.update("min", newValue)
    metrics.update("avg", queue.sum / queue.size)

    (queue, metrics)
  }

  def getUnsignedTSX: Long = {

    val result = requests.get("https://blockchain.info/q/unconfirmedcount")
    val unsignedTSX = ujson.read(result.text).toString().toLong
    unsignedTSX

  }

  def getBlockInfo(blockHeight: Int): mutable.Map[String, Long] = {

    val url = "https://api.blockchair.com/bitcoin/blocks?q=id(".concat(blockHeight.toString).concat(")")
    val result = requests.get(url)
    val json = ujson.read(result.text)
    val data = json("data").arr(0)
    val fee = data("fee_total").toString().toLong
    val tsxCount = data("transaction_count").toString().toLong
    val cryptoVal = data("input_total").toString().toLong
    val unsignedTsxCount = getUnsignedTSX

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