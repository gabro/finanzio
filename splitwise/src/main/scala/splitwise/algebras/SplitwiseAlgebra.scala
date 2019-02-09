package splitwise.algebras

import splitwise.models._

import java.time.Instant

trait SplitwiseAlgebra[F[_]] {
  def getCurrentUser(): F[User]
  def getExpenses(
      groupId: Option[String] = None,
      datedAfter: Option[Instant] = None,
      limit: Option[Int] = None,
  ): F[List[Expense]]
}
