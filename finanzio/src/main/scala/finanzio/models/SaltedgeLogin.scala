package finanzio.models

import io.circe.generic.extras._

import java.time.OffsetDateTime

@ConfiguredJsonCodec case class SaltedgeLogin(id: String)

object SaltedgeLogin {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
