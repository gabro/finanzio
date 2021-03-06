package finanzio.data

import saltedge.models.{Transaction => SaltedgeTransaction}
import saltedge.models.{Account => SaltedgeAccount}
import saltedge.models.{Login => SaltedgeLogin}

import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._

trait SaltedgeRepository[F[_]] {
  def storeTransactions(transactions: List[SaltedgeTransaction]): F[Unit]
  def storeAccounts(account: List[SaltedgeAccount]): F[Unit]
  def storeLogins(logins: List[SaltedgeLogin]): F[Unit]
}

object SaltedgeRepository extends DoobieMappings {

  def create[F[_]: Async: ContextShift](xa: Transactor[F]): SaltedgeRepository[F] =
    new SaltedgeRepository[F] {

      override def storeTransactions(transactions: List[SaltedgeTransaction]): F[Unit] = {
        val q = """
    insert into transactions
    (id, mode, status, madeOn, amount, currencyCode, description, category,
      duplicated, extra, accountId, createdAt, updatedAt
    )
    values
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    on conflict (id) do update set
      mode = excluded.mode,
      status = excluded.status,
      madeOn = excluded.madeOn,
      amount = excluded.amount,
      currencyCode = excluded.currencyCode,
      description = excluded.description,
      category = excluded.category,
      duplicated = excluded.duplicated,
      extra = excluded.extra,
      accountId = excluded.accountId,
      createdAt = excluded.createdAt,
      updatedAt = excluded.updatedAt
    """
        Update[SaltedgeTransaction](q).updateMany(transactions).transact(xa).void
      }

      override def storeAccounts(accounts: List[SaltedgeAccount]): F[Unit] = {
        val q = """
    insert into accounts
    (id, name, nature, balance, currencyCode, loginId, createdAt, updatedAt)
    values
    (?, ?, ?, ?, ?, ?, ?, ?)
    on conflict (id) do update set
      name = excluded.name,
      nature = excluded.nature,
      balance = excluded.balance,
      currencyCode = excluded.currencyCode,
      loginId = excluded.loginId,
      createdAt = excluded.createdAt,
      updatedAt = excluded.updatedAt
    """
        Update[SaltedgeAccount](q).updateMany(accounts).transact(xa).void
      }

      override def storeLogins(logins: List[SaltedgeLogin]): F[Unit] = {
        val q = """
    insert into logins
    (id, providerName)
    values
    (?, ?)
    on conflict (id) do update set
      providerName = excluded.providerName
    """
        Update[(String, String)](q)
          .updateMany(logins.map(login => (login.id, login.providerName)))
          .transact(xa)
          .void
      }

    }

}
