package finanzio.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class SaltedgeLogin(id: String, providerName: String)

object SaltedgeLogin {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
