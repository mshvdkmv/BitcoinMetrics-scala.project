package kek

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


object Kek extends App {

  val request = Map(
    "Binance" -> "https://api.binance.com/api/v3/depth?symbol=BTCUSDT&limit=20",
    "OKEx" -> "http://www.okex.com/api/spot/v3/instruments/BTC-USDT/book?size=20",
    "CoinbasePro" -> "https://api.pro.coinbase.com/products/BTC-USD/book?level=2",
  )

  def scrap_data(request_dict:Map[String,String]):(Vector[(Double, Double)], Vector[(Double, Double)])= {
    var all_asks: Vector[(Double, Double)] = Vector()
    var all_bids: Vector[(Double, Double)] = Vector()
    for ((name, link) <- request_dict) {
      val result = requests.get(link)
      val json = ujson.read(result.text)
      val asks = json("asks").arr
      val bids = json("bids").arr
      for (ask <- asks) {
        val cost: Double = ask(0).str.toDouble
        val amount: Double = ask(1).str.toDouble
        all_asks = (cost, amount) +: all_asks
      }
      for (bid <- bids) {
        val cost : Double = bid(0).str.toDouble
        val amount : Double = bid(1).str.toDouble
        all_bids = (cost, amount) +: all_bids
      }

    }
    (all_asks, all_bids)
  }

  val (a ,b) = scrap_data(request)
  val asks = a.sortBy(-_._1)
  val bids = b.sortBy(-_._1)
  val lt: LocalDateTime = LocalDateTime.now()
  val symbol = "XBTUSD"

  val testEntry : Entry = Entry(lt,symbol,b,a)


  import StreamStages._

  implicit val actors: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = actors.dispatcher

  var c: Long = 1000000

  val source: Source[Entry, NotUsed] = Source(testEntry :: Nil)

  println(source)

  val route = concat(
    (path("instant-metrics") & parameter("symbol".as[String].?)) { symbol: Option[String] =>
      get {
        val stream = source
          .via(filter(symbol))
          .via(instantMetrics(c))
          .map(_.toVector)
          .prepend(instantHeader)
          .via(formatter)
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
      }
    },
    (path("sma") & parameter("symbol".as[String]) & parameter("n".as[Int])) { (symbol: String, n: Int) =>
        get {
          val stream = source
            .via(filter(Some(symbol)))
            .via(instantMetrics(c))
            .sliding(n)
            .via(sma)
            .map(_.toVector)
            .prepend(continuousHeader)
            .via(formatter)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
        }
      },
    (path("ema") & parameter("symbol".as[String]) & parameter("n".as[Int])) { (symbol: String, n: Int) =>
        get {
          val stream = source
            .via(filter(Some(symbol)))
            .via(instantMetrics(c))
            .sliding(n)
            .via(sma)
            .via(smaToEma)
            .map(_.toVector)
            .prepend(continuousHeader)
            .via(formatter)
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, stream))
        }
      },
    (path("c") & parameter("c".as[Long])) { c: Long =>
        post {
          this.c = c
          complete("c changed")
        }
    }
  )

  for {
    binding <- Http().bindAndHandle(route, "localhost", 8080)
    _ = sys.addShutdownHook {
      for {
        _ <- binding.terminate(Duration(5, TimeUnit.SECONDS))
        _ <- actors.terminate()
      } yield ()
    }
  } yield ()
}
