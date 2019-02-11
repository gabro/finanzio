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

import saltedge._
import saltedge.algebras._

import splitwise._
import splitwise.algebras._

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

  val splitwise = for {
    splitwiseConfig <- Resource.liftF(loadConfigF[F, SplitwiseConfig]("splitwise"))
    sw <- Splitwise.create[IO](splitwiseConfig.appId, splitwiseConfig.secret)
  } yield sw

  val interval = 30.minutes

  def app(args: List[String]) =
    blazeClient.use { client =>
      dbTransactor.use { transactor =>
        splitwise.use { splitwise =>
          val loggingClient = Logger(true, true)(client)
          for {
            logger <- Slf4jLogger.create[F]
            migrations <- FlywayMigrations.create[F]
            _ <- new MigrationsCli[F](migrations)(implicitly, logger).run(args)
            saltedgeConfig <- loadConfigF[F, SaltedgeConfig]("saltedge")
            saltedgeService = Saltedge
              .create[F](loggingClient, saltedgeConfig.appId, saltedgeConfig.secret)
            saltedgeRepository = SaltedgeRepository.create[F](transactor)
            splitwiseRepository = SplitwiseRepository.create[F](transactor)
            accounts <- saltedgeService.accounts()
            _ <- runEvery(interval) {
              retrieveAndStoreTransactions(
                saltedgeService,
                saltedgeRepository,
                splitwise,
                splitwiseRepository,
                logger,
              )
            }
          } yield ()
        }
      }
    }

  def retrieveAndStoreTransactions[F[_]: Sync: Par](
      saltedgeService: SaltedgeAlgebra[F],
      saltedgeRepository: SaltedgeRepository[F],
      splitwise: SplitwiseAlgebra[F],
      splitwiseRepository: SplitwiseRepository[F],
      logger: CatsLogger[F],
  ): F[Unit] = {
    val retrieveAndStore = (
      saltedgeService.transactions >>= saltedgeRepository.storeTransactions,
      saltedgeService.accounts >>= saltedgeRepository.storeAccounts,
      saltedgeService.logins >>= saltedgeRepository.storeLogins,
      splitwise.getExpenses(limit = Some(100)) >>= splitwiseRepository.storeExpenses,
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
