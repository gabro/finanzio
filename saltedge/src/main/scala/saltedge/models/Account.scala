package saltedge.models

import io.circe.generic.extras._

import java.time.Instant

@ConfiguredJsonCodec case class Account(
    id: String,
    name: String,
    nature: String,
    balance: Double,
    currencyCode: String,
    loginId: String,
    createdAt: Instant,
    updatedAt: Instant,
)

object Account {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
