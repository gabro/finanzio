package finanzio.data

import finanzio.models._

import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._

trait SaltedgeRepository[F[_]] {
  def store(transactions: List[SaltedgeTransaction]): F[Unit]
}

class SaltedgeRepositoryImpl[F[_]: Async: ContextShift](xa: Transactor[F])
    extends SaltedgeRepository[F] {

  override def store(transactions: List[SaltedgeTransaction]): F[Unit] = {
    val q = """
    insert into transactions
    (id, mode, status, madeOn, amount, currencyCode, description, category,
      duplicated, accountId, createdAt, updatedAt
    )
    values
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    on conflict do nothing
    """
    Update[SaltedgeTransaction](q).updateMany(transactions).transact(xa).void
  }

}
