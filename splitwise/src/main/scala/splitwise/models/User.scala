package splitwise.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class User(
    id: Long,
    firstName: Option[String],
    lastName: Option[String],
)

object User {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
