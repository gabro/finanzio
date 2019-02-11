package saltedge.models

import io.circe.generic.extras._

import java.time.Instant
import java.time.LocalDate

@ConfiguredJsonCodec case class Transaction(
    id: String,
    mode: String,
    status: String,
    madeOn: LocalDate,
    amount: Double,
    currencyCode: String,
    description: String,
    category: String,
    duplicated: Boolean,
    extra: TransactionExtra,
    accountId: String,
    createdAt: Instant,
    updatedAt: Instant,
)

object Transaction {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
