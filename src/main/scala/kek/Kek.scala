package kek

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import java.io._

import akka.http.scaladsl.server.Route

import scala.collection.mutable.{Map}
import StreamStages._
import com.typesafe.config.ConfigFactory

object Kek extends App {


  val conf = ConfigFactory.load()
  var c: Long = 1000000L

  val exchangesMap = Map(
    "Binance" -> conf.getString("exchanges.Binance"),
    "OKEx" -> conf.getString("exchanges.OKEx"),
    "CoinbasePro" -> conf.getString("exchanges.CoinbasePro")
  )


  val (a ,b) = Metrics.scrapExchangesBidsAsks(exchangesMap)
  val lt: LocalDateTime = LocalDateTime.now()
  val symbol = "XBTUSD"
  val testEntry : Entry = Entry(lt,symbol,b,a)
  val exchangesMetrics = Metrics.instantFromEntry(testEntry,c)
  val exchangesString = exchangesMetrics.toString


  var blockchainMetric = BlockchainMetrics.metricsInitialization(5)
  val n = BlockchainMetrics.getLastNHoursHeights(2)
  blockchainMetric = BlockchainMetrics.updateMetrics(blockchainMetric,n)
  val blockchainResult = blockchainMetric.toVector

  val exchangeSource: Source[Entry, NotUsed] = Source(testEntry :: Nil)
  val blockchainSource: Source[Vector[String], NotUsed] = Source(blockchainResult :: Nil)

  val writePath = conf.getString("directories.writePath")
  

  val exchangesPW = new PrintWriter(new File(writePath.concat("exchanges_metrics.txt")))
  exchangesPW.write(exchangesString)
  exchangesPW.close()

  val blockchainPW = new PrintWriter(new File(writePath.concat("blockchain_metrics.txt") ))
  blockchainPW.write(blockchainMetric.toString())
  blockchainPW.close()


  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher


  val route: Route = (path("instant-metrics") & parameter("symbol".as[String].?)) { symbol =>
    get {
      val stream = exchangeSource
        .via(filter(symbol))
        .via(instantMetrics(c))
        .map(_.toVector)
        .prepend(instantHeader)
        .via(formatter)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
    }
  }

  val myRoute: Route = path("blockchain-metrics") {
    get {
      val stream = blockchainSource
        .map(_.toVector)
        .prepend(blockchainHeader)
        .via(formatter)
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
    }
  }


  for {
    binding <- Http().bindAndHandle(route, "localhost", 8080)
    binding <- Http().bindAndHandle(myRoute, "localhost", 8088)

    _ = sys.addShutdownHook {
      for {
        _ <- binding.terminate(Duration(5, TimeUnit.SECONDS))
        _ <- actors.terminate()
      } yield ()
    }
  } yield ()



}
