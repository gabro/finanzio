package saltedge.models

import io.circe.generic.extras._

@ConfiguredJsonCodec case class Attempt(
    interactive: Boolean,
)

object Attempt {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
