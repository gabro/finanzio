package finanzio.models

import io.circe.Json
import io.circe.generic.extras._

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@ConfiguredJsonCodec case class SaltedgeTransaction(
    id: String,
    mode: String,
    status: String,
    madeOn: LocalDate,
    amount: Double,
    currencyCode: String,
    description: String,
    category: String,
    duplicated: Boolean,
    extra: SaltedgeTransactionExtra,
    accountId: String,
    createdAt: Instant,
    updatedAt: Instant,
)

object SaltedgeTransaction {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}

@ConfiguredJsonCodec case class SaltedgeTransactionExtra(
    id: Option[String],
    merchantId: Option[String],
    recordNumber: Option[String],
    information: Option[String],
    time: Option[LocalTime],
    postingDate: Option[LocalDate],
    convert: Option[Boolean],
    transferAccountName: Option[String],
    accountNumber: Option[String],
    originalAmount: Option[Double],
    originalCurrencyCode: Option[String],
    assetCode: Option[String],
    assetAmount: Option[Double],
    originalCategory: Option[String],
    originalSubcategory: Option[String],
    customerCategoryCode: Option[String],
    customerCategoryName: Option[String],
    possibleDuplicate: Option[Boolean],
    tags: Option[List[String]],
    mcc: Option[String],
    payee: Option[String],
    payeeInformation: Option[String],
    payer: Option[String],
    payerInformatino: Option[String],
    `type`: Option[String],
    checkNumber: Option[String],
    units: Option[Double],
    additional: Option[String],
    unitPrice: Option[Double],
    accountBalanceSnapshot: Option[Double],
    categorizationConfidence: Option[Double],
    variableCode: Option[String],
    specificCode: Option[String],
    constantCode: Option[String],
    closingBalance: Option[Double],
    openingBalance: Option[Double],
)

object SaltedgeTransactionExtra {
  implicit val circeConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
