package saltedge

import models._
import algebras._

import cats.implicits._
import cats.effect._
import cats.temp.par._
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._
import io.circe.Json
import java.time.Instant

object Saltedge {
  def create[F[_]: Async: Par](
      httpClient: Client[F],
      appId: String,
      secret: String,
  ): SaltedgeAlgebra[F] = new SaltedgeAlgebra[F] {

    private val baseUri = Uri.uri("https://www.saltedge.com") / "api" / "v4"

    override def logins(): F[List[Login]] =
      for {
        json <- httpClient.expect[Json](saltedgeRequest(baseUri / "logins"))
        logins <- IO.fromEither(json.hcursor.downField("data").as[List[Login]]).to[F]
      } yield logins

    override def accounts(): F[List[Account]] =
      for {
        logins <- logins()
        accounts <- logins.parFlatTraverse(accountsByLogin)
      } yield accounts

    override def transactions(): F[List[Transaction]] =
      for {
        logins <- logins()
        _ <- logins.parTraverse(refreshLogin)
        transactions <- logins.parFlatTraverse(transactionsByLogin)
      } yield transactions

    private def accountsByLogin(login: Login): F[List[Account]] = {
      val uri =
        (baseUri / "accounts").withQueryParam("login_id", login.id)
      val req = saltedgeRequest(uri)
      for {
        json <- httpClient.expect[Json](req)
        accounts <- IO
          .fromEither(json.hcursor.downField("data").as[List[Account]])
          .to[F]
      } yield accounts
    }

    private def transactionsByLogin(login: Login): F[List[Transaction]] = {
      val uri =
        (baseUri / "transactions")
          .withQueryParam("login_id", login.id)
          .withQueryParam("per_page", 1000)
      val req = saltedgeRequest(uri)
      for {
        json <- httpClient.expect[Json](req)
        transactions <- IO
          .fromEither(json.hcursor.downField("data").as[List[Transaction]])
          .to[F]
      } yield transactions
    }

    private def refreshLogin(login: Login): F[Unit] = {
      val canRefresh = login.nextRefreshPossibleAt.exists(_.isBefore(Instant.now))
      if (!login.lastAttempt.interactive && canRefresh) {
        val uri = baseUri / "logins" / login.id / "refresh"
        val req = saltedgeRequest(uri, Method.PUT)
        httpClient.expect[Json](req).void
      } else {
        Sync[F].unit
      }
    }

    private def saltedgeRequest(uri: Uri, method: Method = Method.GET): Request[F] = Request(
      method = method,
      uri = uri,
      headers = Headers(
        Header("App-id", appId),
        Header("Secret", secret),
      ),
    )
  }

}
