package trading.snapshots

import cats.effect.kernel.Async
import cats.syntax.all.*
import ciris.*
import dev.profunktor.pulsar.Config as PulsarConfig

import trading.domain.*

final case class SnapshotsConfig(
    pulsar: PulsarConfig,
    redisUri: RedisURI
)

object Config:
  def load[F[_]: Async]: F[SnapshotsConfig] =
    (
      env("PULSAR_URI").as[PulsarURI].fallback("pulsar://localhost:6650"),
      env("REDIS_URI").as[RedisURI].fallback("redis://localhost").covary[F]
    ).parMapN { (pulsarUri, redisUri) =>
      val pulsar =
        PulsarConfig.Builder
          .withTenant("public")
          .withNameSpace("default")
          .withURL(pulsarUri.value)
          .build
      SnapshotsConfig(pulsar, redisUri)
    }.load[F]