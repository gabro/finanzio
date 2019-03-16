package saltedge.models

import io.circe.generic.extras._
import io.circe.java8.time._

import java.time.Instant

@ConfiguredJsonCodec case class Login(
    id: String,
    providerName: String,
    nextRefreshPossibleAt: Option[Instant],
    lastAttempt: Attempt,
)

object Login {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
