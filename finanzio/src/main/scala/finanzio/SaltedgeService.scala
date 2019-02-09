package finanzio

import models._
import config.SaltedgeConfig

import cats.implicits._
import cats.effect._
import cats.temp.par._
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._
import io.circe.Json

trait SaltedgeService[F[_]] {
  def logins(): F[List[SaltedgeLogin]]
  def accounts(): F[List[SaltedgeAccount]]
  def transactions(): F[List[SaltedgeTransaction]]
}

class SaltedgeServiceImpl[F[_]: Async: Par](
    httpClient: Client[F],
    saltedgeConfig: SaltedgeConfig,
) extends SaltedgeService[F] {

  private val baseUri = Uri.uri("https://www.saltedge.com") / "api" / "v4"

  override def logins(): F[List[SaltedgeLogin]] =
    for {
      json <- httpClient.expect[Json](saltedgeRequest(baseUri / "logins"))
      logins <- IO.fromEither(json.hcursor.downField("data").as[List[SaltedgeLogin]]).to[F]
    } yield logins

  override def accounts(): F[List[SaltedgeAccount]] =
    for {
      logins <- logins()
      accounts <- logins.parTraverse(accountsByLogin).map(_.flatten)
    } yield accounts

  override def transactions(): F[List[SaltedgeTransaction]] =
    for {
      logins <- logins()
      transactions <- logins.parTraverse(transactionsByLogin).map(_.flatten)
    } yield transactions

  private def accountsByLogin(login: SaltedgeLogin): F[List[SaltedgeAccount]] = {
    val uri =
      (baseUri / "accounts").withQueryParam("login_id", login.id)
    val req = saltedgeRequest(uri)
    for {
      json <- httpClient.expect[Json](req)
      accounts <- IO
        .fromEither(json.hcursor.downField("data").as[List[SaltedgeAccount]])
        .to[F]
    } yield accounts
  }

  private def transactionsByLogin(login: SaltedgeLogin): F[List[SaltedgeTransaction]] = {
    val uri =
      (baseUri / "transactions").withQueryParam("login_id", login.id)
    val req = saltedgeRequest(uri)
    for {
      json <- httpClient.expect[Json](req)
      transactions <- IO
        .fromEither(json.hcursor.downField("data").as[List[SaltedgeTransaction]])
        .to[F]
    } yield transactions
  }

  private def saltedgeRequest(uri: Uri, method: Method = Method.GET): Request[F] = Request(
    method = method,
    uri = uri,
    headers = Headers(
      Header("App-id", saltedgeConfig.appId),
      Header("Secret", saltedgeConfig.secret),
    ),
  )

}
