package finanzio

import config._
import data._

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import org.http4s.client.blaze._
import org.http4s.client._
import org.http4s.client.middleware.Logger
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import doobie._
import doobie.hikari._
import doobie.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  implicit private def pureConfigHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  type E[A] = IO[A]

  implicit val parallelIO: Parallel[IO, IO] = Parallel[IO, IO.Par].asInstanceOf[Parallel[IO, IO]]

  val dbTransactor: Resource[E, HikariTransactor[E]] =
    for {
      connectExecutionContext <- ExecutionContexts.fixedThreadPool[E](32)
      transactionExecutionContext <- ExecutionContexts.cachedThreadPool[E]
      transactor <- HikariTransactor.newHikariTransactor[E](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/finanzio",
        "finanzio",
        "finanzio",
        connectExecutionContext,
        transactionExecutionContext
      )
    } yield transactor

  val blazeClient: Resource[E, Client[E]] = BlazeClientBuilder[E](global).resource

  private val app =
    blazeClient.use { client =>
      dbTransactor.use { transactor =>
        val loggingClient = Logger(true, true)(client)
        for {
          logger <- Slf4jLogger.create[E]
          _ <- FlywayMigrations.create[E]
          saltedgeConfig <- loadConfigF[E, SaltedgeConfig]("saltedge")
          saltedgeService = new SaltedgeServiceImpl[E](loggingClient, saltedgeConfig)
          saltedgeRepository = new SaltedgeRepositoryImpl[E](transactor)
          transactions <- saltedgeService.transactions()
          _ <- saltedgeRepository.store(transactions)
          _ <- logger.info(transactions.groupBy(_.accountId).mapValues(_.length).toString)
        } yield ()
      }
    }

  def run(args: List[String]): IO[ExitCode] =
    app.as(ExitCode.Success)

}
