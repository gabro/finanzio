package splitwise.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class ExpenseUser(
    user: User,
    userId: Long,
    paidShare: String,
    owedShare: String,
    netBalance: String,
)

object ExpenseUser {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
