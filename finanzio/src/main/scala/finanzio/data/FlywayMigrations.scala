package finanzio.data

import cats.syntax.all._
import cats.instances.list._
import cats.effect._
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.module.catseffect._
import pureconfig.generic.auto._
import org.flywaydb.core.Flyway
import io.chrisdavenport.log4cats.Logger
import finanzio.config._

case class Migration(
    state: String,
    version: String,
    description: String,
)

trait Migrations[F[_]] {
  def list(): F[List[Migration]]
  def clean(): F[Unit]
  def migrate(): F[Unit]
}

final class MigrationsCli[F[_]](
    migrations: Migrations[F],
)(implicit F: Sync[F], logger: Logger[F]) {

  def run(args: List[String]): F[Unit] =
    args.traverse {
      case "info"    => info()
      case "clean"   => migrations.clean()
      case "migrate" => migrations.migrate()
      case _         => F.unit
    }.void

  private def info(): F[Unit] =
    for {
      list <- migrations.list()
      _ <- logger.info(
        list.mkString(
          start = "Migrations:\n",
          sep = "\n",
          end = s"\n${list.length} migrations found",
        ),
      )
    } yield ()
}

private class FlywayMigrations[F[_]](
    flyway: Flyway,
)(implicit F: Sync[F])
    extends Migrations[F] {

  override def list(): F[List[Migration]] = F.delay {
    flyway.info.all.map { m =>
      Migration(
        state = m.getState.toString,
        version = m.getVersion.toString,
        description = m.getDescription,
      )
    }.toList
  }

  override def clean(): F[Unit] = F.delay {
    flyway.clean
  }

  override def migrate(): F[Unit] =
    F.delay {
      flyway.migrate
    }.void

}

object FlywayMigrations {

  implicit private def pureConfigHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  def create[F[_]](implicit F: Sync[F]): F[Migrations[F]] =
    for {
      dbConfig <- loadConfigF[F, DbConfig]("db")
      flywayConfig <- loadConfigF[F, FlywayConfig]("flyway")
      flywayMigrations <- F.delay {
        val flyway = Flyway
          .configure()
          .dataSource(
            dbConfig.url,
            dbConfig.user,
            dbConfig.password,
          )
          .locations(flywayConfig.locations)
          .load()
        new FlywayMigrations(flyway)
      }
    } yield flywayMigrations
}
