package oauth

import cats.effect._
import io.circe.Json
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._

import models._

trait OAuth[F[_]] {
  def getAuthorizeUrl(params: Map[String, String]): F[Uri]
  def buildAuthHeader(tokens: Tokens): String
  def getAccessToken(code: String, params: Map[String, String]): F[Tokens]
}

object OAuth {
  def create[F[_]](
      httpClient: Client[F],
      clientId: String,
      clientSecret: String,
      baseSite: Uri,
      authorizePath: List[Uri.Path],
      accessTokenPath: List[Uri.Path],
  )(implicit F: Sync[F]): OAuth[F] = new OAuth[F] {

    override def getAuthorizeUrl(params: Map[String, String]): F[Uri] = F.delay {
      authorizePath
        .foldLeft(baseSite)(_ / _)
        .setQueryParams(params.mapValues(List(_)))
        .withQueryParam("client_id", clientId)
    }

    override def buildAuthHeader(tokens: Tokens) =
      s"$authMethod ${tokens.accessToken}"

    override def getAccessToken(code: String, params: Map[String, String]): F[Tokens] = {
      val codeParam = params.get("grant_type") match {
        case Some("refresh_token") => "refresh_token"
        case _                     => "code"
      }

      val uri = accessTokenPath
        .foldLeft(baseSite)(_ / _)
        .setQueryParams(params.mapValues(List(_)))
        .withQueryParam("client_id", clientId)
        .withQueryParam("client_secret", clientSecret)
        .withQueryParam(codeParam, code)

      val req = Request[F](
        method = Method.POST,
        uri = uri,
        headers = Headers(
          Header("Content-Type", "application/x-www-form-urlencoded"),
        ),
      )

      httpClient.expect[Tokens](req)
    }

    private val authMethod = "Bearer"
    private val accessTokenName = "access_token"

  }
}
