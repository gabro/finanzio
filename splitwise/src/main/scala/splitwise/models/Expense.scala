package splitwise.models

import io.circe.generic.extras._

import java.time.Instant

@ConfiguredJsonCodec case class Expense(
    id: Long,
    groupId: Option[Long],
    description: String,
    repeats: Boolean,
    repeatInterval: Option[String],
    details: Option[String],
    cost: Double,
    currencyCode: String,
    date: Instant,
    createdBy: User,
    users: List[ExpenseShare],
    payment: Boolean,
)

object Expense {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
