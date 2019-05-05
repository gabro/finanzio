package finanzio.data

import cats.implicits._
import cats.effect._
import doobie._
import doobie.implicits._

import splitwise.models.Expense
import splitwise.models.User
import splitwise.models.ExpenseShare
import saltedge.models.Transaction

import java.time.Instant

trait SplitwiseRepository[F[_]] {
  def storeExpenses(expenses: List[Expense]): F[Unit]
  def storeExpenseMatches(expenseMatches: List[(Transaction, Expense, ExpenseShare)]): F[Unit]
}

object SplitwiseRepository extends DoobieMappings {

  def create[F[_]: Async: ContextShift](xa: Transactor[F]): SplitwiseRepository[F] =
    new SplitwiseRepository[F] {

      private def storeUsers(users: List[User]): ConnectionIO[Int] = {
        val q = """
        insert into splitwise_users
        (id, firstName, lastName)
        values
        (?, ?, ?)
        on conflict (id) do update set
          firstName = excluded.firstName,
          lastName = excluded.lastName
        """
        Update[User](q).updateMany(users)
      }

      private def storeExpenseShares(
          expenseShares: List[(Long, ExpenseShare)],
      ): ConnectionIO[Int] = {
        val q = """
        insert into splitwise_expense_shares
        (expenseId, userId, paidShare, owedShare, netBalance)
        values
        (?, ?, ?, ?, ?)
        on conflict (expenseId, userId) do update set
          paidShare = excluded.paidShare,
          owedShare = excluded.owedShare,
          netBalance = excluded.netBalance
        """
        Update[(Long, DbExpenseShare)](q)
          .updateMany(expenseShares.map {
            case (expenseId, expenseShare) =>
              (expenseId, DbExpenseShare.fromExpenseShare(expenseShare))
          })
      }

      override def storeExpenses(expenses: List[Expense]): F[Unit] = {
        val q = """
        insert into splitwise_expenses
        (id, groupId, description, repeats, repeatInterval, details, cost, currencyCode,
          date, createdBy, payment)
        values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (id) do update set
            groupId = excluded.groupId,
            description = excluded.description,
            repeats = excluded.repeats,
            repeatInterval = excluded.repeatInterval,
            details = excluded.details,
            cost = excluded.cost,
            currencyCode = excluded.currencyCode,
            date = excluded.date,
            createdBy = excluded.createdBy,
            payment = excluded.payment
        """
        val interestingExpenses =
          expenses.filterNot(_.description == "Settle all balances")
        val knownUsers =
          interestingExpenses
            .flatMap(expense => expense.createdBy :: expense.users.map(_.user))
            .distinct
        val expenseShares = interestingExpenses.flatMap(e => e.users.map((e.id, _))).distinct
        val dbExpenses = interestingExpenses.map(DbExpense.fromExpense)

        val transaction =
          storeUsers(knownUsers) >>
            Update[DbExpense](q).updateMany(dbExpenses) >>
            storeExpenseShares(expenseShares)

        transaction.transact(xa).void
      }

      def storeExpenseMatches(
          expenseMatches: List[(Transaction, Expense, ExpenseShare)],
      ): F[Unit] = {
        val q = """
        insert into splitwise_matched_transactions
        (saltedge_transaction_id, splitwise_expense_id, splitwise_user_id)
        values
        (?, ?, ?)
        on conflict (saltedge_transaction_id) do update set
            splitwise_expense_id = excluded.splitwise_expense_id,
            splitwise_user_id = excluded.splitwise_user_id
        """

        val matchedTransactions = expenseMatches.map {
          case (transaction, expense, share) =>
            (transaction.id, expense.id, share.userId)
        }

        Update[(String, Long, Long)](q).updateMany(matchedTransactions).transact(xa).void
      }

    }

  case class DbExpense(
      id: Long,
      groupId: Option[Long],
      description: String,
      repeats: Boolean,
      repeatInterval: Option[String],
      details: Option[String],
      cost: Double,
      currencyCode: String,
      date: Instant,
      createdBy: Long,
      payment: Boolean,
  )

  object DbExpense {
    def fromExpense(expense: Expense): DbExpense = DbExpense(
      id = expense.id,
      groupId = expense.groupId,
      description = expense.description,
      repeats = expense.repeats,
      repeatInterval = expense.repeatInterval,
      details = expense.details,
      cost = expense.cost,
      currencyCode = expense.currencyCode,
      date = expense.date,
      createdBy = expense.createdBy.id,
      payment = expense.payment,
    )
  }

  case class DbExpenseShare(
      userId: Long,
      paidShare: Double,
      owedShare: Double,
      netBalance: Double,
  )
  object DbExpenseShare {
    def fromExpenseShare(expenseShare: ExpenseShare): DbExpenseShare = DbExpenseShare(
      userId = expenseShare.userId,
      paidShare = expenseShare.paidShare,
      owedShare = expenseShare.owedShare,
      netBalance = expenseShare.netBalance,
    )
  }

}
