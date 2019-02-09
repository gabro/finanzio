package splitwise

import algebras._
import models._

import oauth._

import cats.syntax.all._
import cats.effect._
import io.circe._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.middleware.Logger
import org.http4s.circe.CirceEntityDecoder._

import scala.concurrent.ExecutionContext.global
import java.time.Instant

object Splitwise {
  def create[F[_]](
      key: String,
      secret: String,
  )(implicit F: ConcurrentEffect[F]): Resource[F, SplitwiseAlgebra[F]] =
    BlazeClientBuilder[F](global).resource.map { httpClient =>
      val oauth = OAuth.create[F](
        Logger(true, true)(httpClient),
        clientId = key,
        clientSecret = secret,
        Uri.uri("https://secure.splitwise.com/"),
        List("oauth", "authorize"),
        List("oauth", "token"),
      )
      val baseUri = Uri.uri("https://secure.splitwise.com/api/v3.0/")
      new SplitwiseAlgebra[F] {
        def getCurrentUser(): F[User] =
          splitwiseRequest("get_current_user", "user")

        def getExpenses(
            groupId: Option[String] = None,
            datedAfter: Option[Instant] = None,
            limit: Option[Int] = None,
        ): F[List[Expense]] =
          splitwiseRequest(
            "get_expenses",
            "expenses",
            uri =>
              uri
                .withOptionQueryParam("groupId", groupId)
                .withOptionQueryParam("datedAfter", datedAfter.map(_.toString))
                .withOptionQueryParam("limit", limit),
          )

        private def splitwiseRequest[A: Decoder](
            path: Uri.Path,
            responseKey: String,
            uriModifier: Uri => Uri = identity,
        ): F[A] =
          for {
            tokens <- oauth.getAccessToken(
              "",
              Map("grant_type" -> "client_credentials"),
            )
            req = Request[F](
              method = Method.GET,
              uri = uriModifier(baseUri / path),
              headers = Headers(
                Header("Authorization", oauth.buildAuthHeader(tokens)),
              ),
            )
            json <- httpClient.expect[Json](req)
            user <- IO.fromEither(json.hcursor.downField(responseKey).as[A]).to[F]
          } yield user
      }
    }

}
