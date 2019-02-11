package saltedge.algebras

import saltedge.models._

trait SaltedgeAlgebra[F[_]] {
  def logins(): F[List[Login]]
  def accounts(): F[List[Account]]
  def transactions(): F[List[Transaction]]
}
