package splitwise.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class Expense(
    id: Long,
    groupId: Long,
    description: String,
    repeats: Boolean,
    repeatInterval: String,
    details: Option[String],
    cost: String,
    currencyCode: String,
    date: String,
    createdBy: User,
    users: List[ExpenseUser],
    payment: Boolean,
)

object Expense {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
