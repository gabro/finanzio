package finanzio.data

import saltedge.models.{TransactionExtra => SaltedgeTransactionExtra}

import cats.syntax.either._
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import doobie._
import org.postgresql.util.PGobject

trait DoobieMappings {
  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(
        a => {
          val o = new PGobject
          o.setType("json")
          o.setValue(a.spaces2)
          o
        },
      )

  implicit val transactionExtraMeta: Meta[SaltedgeTransactionExtra] =
    Meta[Json].timap(_.as[SaltedgeTransactionExtra].right.get)(_.asJson)

}
