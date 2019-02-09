package finanzio

import config._
import data._

import cats.implicits._
import cats.effect._
import cats.temp.par._
import org.http4s.client.blaze._
import org.http4s.client._
import org.http4s.client.middleware.Logger
import pureconfig.generic.auto._
import pureconfig.module.catseffect._
import io.chrisdavenport.log4cats.{Logger => CatsLogger}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import doobie._
import doobie.hikari._
import fs2.Stream

import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Main extends IOApp {

  type F[A] = IO[A]

  val dbTransactor: Resource[F, HikariTransactor[F]] =
    for {
      connectExecutionContext <- ExecutionContexts.fixedThreadPool[F](32)
      transactionExecutionContext <- ExecutionContexts.cachedThreadPool[F]
      dbConfig <- Resource.liftF(loadConfigF[F, DbConfig]("db"))
      transactor <- HikariTransactor.newHikariTransactor[F](
        dbConfig.driver,
        dbConfig.url,
        dbConfig.user,
        dbConfig.password,
        connectExecutionContext,
        transactionExecutionContext,
      )
    } yield transactor

  val blazeClient: Resource[F, Client[F]] = BlazeClientBuilder[F](global).resource

  val interval = 30.minutes

  def app(args: List[String]) =
    blazeClient.use { client =>
      dbTransactor.use { transactor =>
        val loggingClient = Logger(true, true)(client)
        for {
          logger <- Slf4jLogger.create[F]
          migrations <- FlywayMigrations.create[F]
          _ <- new MigrationsCli[F](migrations)(implicitly, logger).run(args)
          saltedgeConfig <- loadConfigF[F, SaltedgeConfig]("saltedge")
          saltedgeService = new SaltedgeServiceImpl[F](loggingClient, saltedgeConfig)
          saltedgeRepository = new SaltedgeRepositoryImpl[F](transactor)
          accounts <- saltedgeService.accounts()
          _ <- runEvery(interval) {
            retrieveAndStoreTransactions(saltedgeService, saltedgeRepository, logger)
          }
        } yield ()
      }
    }

  def retrieveAndStoreTransactions[F[_]: Sync: Par](
      saltedgeService: SaltedgeService[F],
      saltedgeRepository: SaltedgeRepository[F],
      logger: CatsLogger[F],
  ): F[Unit] = {
    val retrieveAndStore = (
      saltedgeService.transactions >>= saltedgeRepository.storeTransactions,
      saltedgeService.accounts >>= saltedgeRepository.storeAccounts,
      saltedgeService.logins >>= saltedgeRepository.storeLogins,
    ).parTupled
    val program =
      logger.info("Retrieving transactions, accounts and logins...") >>
        retrieveAndStore >>
        logger.info(s"Done! Sleeping for $interval now...")
    program.recoverWith {
      case NonFatal(e) =>
        logger.error(e.getMessage) >>
          logger.error(
            s"Error while retrieving and storing transactions. Retrying in $interval...",
          )
    }
  }

  def runEvery[F[_]: Timer: Sync, A](interval: FiniteDuration)(program: F[A]): F[Unit] =
    (Stream.eval(program) ++ Stream.fixedDelay[F](interval).evalMap(_ => program)).compile.drain

  def run(args: List[String]): IO[ExitCode] =
    app(args).as(ExitCode.Success)

}
