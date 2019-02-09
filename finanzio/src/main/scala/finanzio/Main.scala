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
import io.chrisdavenport.log4cats.{Logger => CatsLogger}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import doobie._
import doobie.hikari._
import doobie.implicits._
import fs2.Stream

import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Main extends IOApp {

  implicit private def pureConfigHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  type F[A] = IO[A]

  implicit val parallelIO: Parallel[IO, IO] = Parallel[IO, IO.Par].asInstanceOf[Parallel[IO, IO]]

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
        transactionExecutionContext
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
          _ <- logger.info(
            accounts
              .map(account => s"\nName: ${account.name}\nBalance: ${account.balance}")
              .mkString("\n---")
          )
          _ <- runEvery(interval) {
            retrieveAndStoreTransactions(saltedgeService, saltedgeRepository, logger)
          }
        } yield ()
      }
    }

  def retrieveAndStoreTransactions[F[_]: Sync](
      saltedgeService: SaltedgeService[F],
      saltedgeRepository: SaltedgeRepository[F],
      logger: CatsLogger[F]
  ): F[Unit] =
    (for {
      _ <- logger.info("Retrieving transactions...")
      transactions <- saltedgeService.transactions()
      _ <- saltedgeRepository.store(transactions)
      _ <- logger.info(s"Done! Sleeping for $interval now...")
    } yield ()).recoverWith {
      case NonFatal(e) =>
        logger.error(e.getMessage) >>
          logger.error(
            s"Error while retrieving and storing transactions. Retrying in $interval..."
          )
    }

  def runEvery[F[_]: Timer: Sync, A](interval: FiniteDuration)(program: F[A]): F[Unit] =
    (Stream.eval(program) ++ Stream.fixedDelay[F](interval).evalMap(_ => program)).compile.drain

  def run(args: List[String]): IO[ExitCode] =
    app(args).as(ExitCode.Success)

}
