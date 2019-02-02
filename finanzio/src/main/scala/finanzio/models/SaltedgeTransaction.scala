package finanzio.models

import io.circe.generic.extras._

import java.time.Instant

@ConfiguredJsonCodec case class SaltedgeTransaction(
    id: String,
    mode: String,
    status: String,
    madeOn: String,
    amount: Double,
    currencyCode: String,
    description: String,
    category: String,
    duplicated: Boolean,
    accountId: String,
    createdAt: Instant,
    updatedAt: Instant
)

object SaltedgeTransaction {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
