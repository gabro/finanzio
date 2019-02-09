package oauth.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class Tokens(
    accessToken: String,
    tokenType: String,
)

object Tokens {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
