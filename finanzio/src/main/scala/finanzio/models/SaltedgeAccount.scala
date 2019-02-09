package finanzio.models

import io.circe.generic.extras._

import java.time.Instant

@ConfiguredJsonCodec case class SaltedgeAccount(
    id: String,
    name: String,
    nature: String,
    balance: Double,
    currencyCode: String,
    loginId: String,
    createdAt: Instant,
    updatedAt: Instant
)

object SaltedgeAccount {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
