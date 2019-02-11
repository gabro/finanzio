package saltedge.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class Login(id: String, providerName: String)

object Login {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
