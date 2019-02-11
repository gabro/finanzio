package splitwise.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class ExpenseShare(
    user: User,
    userId: Long,
    paidShare: Double,
    owedShare: Double,
    netBalance: Double,
)

object ExpenseShare {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
