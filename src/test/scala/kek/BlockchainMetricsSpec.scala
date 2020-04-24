package kek

import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

class BlockchainMetricsSpec extends AnyFlatSpec with Matchers with OptionValues{
  import BlockchainMetricsSpec._

  "metricsInitialization" should "be correct" in {

    val test = BlockchainMetrics.metricsInitialization(4)


    test.feeQueue shouldEqual defaultQueue
    test.cryptoValQueue shouldEqual defaultQueue
    test.tsxQueue shouldEqual defaultQueue
    test.unsignedTsxQueue shouldEqual defaultQueue
    test.feeMetrics shouldEqual defaultMetrics
    test.cryptoValMetrics shouldEqual defaultMetrics
    test.tsxMetrics shouldEqual defaultMetrics
    test.unsignedTsxMetrics shouldEqual defaultMetrics
    test.lastBlockHeight shouldEqual 0
  }


  val BM: BlockchainMetrics = BlockchainMetrics.metricsInitialization(3)

  "updateMetrics" should "be correct" in {

    val add : Long = 600L
    val (unsignedTsxQueueNew,unsignedTsxMetricsNew) = BlockchainMetrics.updateMetric(add,BM.unsignedTsxQueue,BM.unsignedTsxMetrics)
    unsignedTsxQueueNew shouldEqual updatedUnsignedTsxQueue
    unsignedTsxMetricsNew shouldEqual updatedUnsignedTsxMetrics
  }


  "getBlockInfo" should "be correct" in {

    val blockInfo = BlockchainMetrics.getBlockInfo(627437)
    //unsignedTsxCount value is dynamic, without this line it is not possible to test this function
    blockInfo.update("unsignedTsxCount",7324)
    blockInfo shouldEqual block627437info
  }


}

object BlockchainMetricsSpec {

  private val defaultQueue = mutable.ArrayDeque[Long]()
  for (_ <- 1 to 4){
    defaultQueue += 0
  }
  private val defaultMetrics = scala.collection.mutable.Map("avg" -> 0.0, "min" -> Double.MaxValue, "max" -> Double.MinValue, "sum" -> 0.0)

  private var updatedUnsignedTsxQueue = mutable.ArrayDeque[Long]()
  updatedUnsignedTsxQueue += 0
  updatedUnsignedTsxQueue += 0
  updatedUnsignedTsxQueue += 600
  private val updatedUnsignedTsxMetrics = scala.collection.mutable.Map("avg" -> 200.0, "min" -> 600.0, "max" -> 600.0, "sum" -> 600.0)
  private val block627437info = scala.collection.mutable.Map("tsxCount" -> 152, "cryptoVal" -> 15281331057L, "unsignedTsxCount" -> 7324, "fee" -> 5792409)

}