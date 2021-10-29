package trading.domain

import java.time.Instant
import java.util.UUID

import trading.commands.TradeCommand
import trading.domain.*
import trading.state.*

import cats.Order
import org.scalacheck.{ Arbitrary, Cogen, Gen }

object arbitraries:
  import generators.*

  given Arbitrary[CommandId]    = Arbitrary(commandIdGen)
  given Arbitrary[TradeCommand] = Arbitrary(tradeCommandGen)
  given Arbitrary[Prices]       = Arbitrary(pricesGen)
  given Arbitrary[Price]        = Arbitrary(priceGen)
  given Arbitrary[Quantity]     = Arbitrary(quantityGen)
  given Arbitrary[TradeState]   = Arbitrary(tradeStateGen)

object cogen:
  given uuidCogen: Cogen[UUID] =
    Cogen[(Long, Long)].contramap { uuid =>
      uuid.getLeastSignificantBits -> uuid.getMostSignificantBits
    }

  given Cogen[CommandId] =
    uuidCogen.contramap(_.value)

  given Cogen[Quantity] =
    Cogen.cogenInt.contramap[Quantity](_.value)

  given Ordering[Price] =
    Order[Price].toOrdering

  given Cogen[Price] =
    Cogen.bigDecimal.contramap[Price](_.value)

  given Cogen[Map[Price, Quantity]] =
    Cogen.cogenMap[BigDecimal, Int].contramap[Map[Price, Quantity]] {
      _.map { (k, v) => k.value -> v.value }
    }

  given Cogen[Prices] =
    Cogen.tuple2[Prices.Ask, Prices.Bid].contramap[Prices] { p =>
      p.ask -> p.bid
    }

object generators:

  val tradeActionGen: Gen[TradeAction] =
    Gen.oneOf(TradeAction.Ask, TradeAction.Bid)

  val commandIdGen: Gen[CommandId] = Gen.uuid.map(id => CommandId(id))

  val eventIdGen: Gen[EventId] = Gen.uuid.map(id => EventId(id))

  val symbolGen: Gen[Symbol] =
    Gen
      .oneOf("EURPLN", "GBPUSD", "CADUSD", "EURUSD", "CHFUSD", "CHFEUR")
      .map(s => Symbol.apply(s))

  val priceGen: Gen[Price] =
    Gen.choose(0.78346, 4.78341).map(x => Price(x))

  val quantityGen: Gen[Quantity] =
    Gen.choose(1, 30).map(Quantity(_))

  val sourceGen: Gen[Source] =
    Gen.const("random-feed")

  val timestampGen: Gen[Timestamp] =
    Gen.const(Timestamp(Instant.parse("2021-09-16T14:00:00.00Z")))

  val createCommandGen: Gen[TradeCommand.Create] =
    for
      i <- commandIdGen
      s <- symbolGen
      a <- tradeActionGen
      p <- priceGen
      q <- quantityGen
      c <- sourceGen
      t <- timestampGen
    yield TradeCommand.Create(i, s, a, p, q, c, t)

  val updateCommandGen: Gen[TradeCommand.Update] =
    for
      i <- commandIdGen
      s <- symbolGen
      a <- tradeActionGen
      p <- priceGen
      q <- quantityGen
      c <- sourceGen
      t <- timestampGen
    yield TradeCommand.Update(i, s, a, p, q, c, t)

  val deleteCommandGen: Gen[TradeCommand.Delete] =
    for
      i <- commandIdGen
      s <- symbolGen
      a <- tradeActionGen
      p <- priceGen
      c <- sourceGen
      t <- timestampGen
    yield TradeCommand.Delete(i, s, a, p, c, t)

  val startCommandGen: Gen[TradeCommand.Start] =
    for
      i <- commandIdGen
      t <- timestampGen
    yield TradeCommand.Start(i, t)

  val stopCommandGen: Gen[TradeCommand.Stop] =
    for
      i <- commandIdGen
      t <- timestampGen
    yield TradeCommand.Stop(i, t)

  val tradeCommandGen: Gen[TradeCommand] =
    Gen.frequency(
      2 -> Gen.oneOf(startCommandGen, stopCommandGen),
      9 -> Gen.oneOf(createCommandGen, updateCommandGen, deleteCommandGen)
    )

  def commandsGen: List[TradeCommand] =
    Gen.listOfN(10, tradeCommandGen).sample.toList.flatten

  // ------ TradeState ------

  val askPricesGen: Gen[Prices.Ask] =
    Gen.mapOf[AskPrice, Quantity] {
      for
        p <- priceGen
        q <- quantityGen
      yield p -> q
    }

  val bidPricesGen: Gen[Prices.Bid] =
    Gen.mapOf[BidPrice, Quantity] {
      for
        p <- priceGen
        q <- quantityGen
      yield p -> q
    }

  val pricesGen: Gen[Prices] =
    for
      a <- askPricesGen
      b <- bidPricesGen
      h <- priceGen
      l <- priceGen
    yield Prices(a, b, h, l)

  val tradingStatusGen: Gen[TradingStatus] =
    Gen.oneOf(TradingStatus.On, TradingStatus.Off)

  val tradeStateGen: Gen[TradeState] =
    Gen
      .mapOf[Symbol, Prices] {
        for
          s <- symbolGen
          p <- pricesGen
        yield s -> p
      }
      .flatMap { kv =>
        tradingStatusGen.map { st =>
          TradeState(st, kv)
        }
      }
