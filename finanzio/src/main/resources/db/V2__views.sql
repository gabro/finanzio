CREATE OR REPLACE VIEW transactions_metabase AS
 SELECT
    transactions.id,
    transactions.mode,
    transactions.status,
    transactions.madeon,
    - transactions.amount as amount,
    transactions.currencycode,
    transactions.description,
    transactions.category,
    transactions.duplicated,
    transactions.extra,
    transactions.accountid,
    logins.providername as login_name,
    accounts.name AS account_name,
    accounts.nature AS account_nature,
    transactions.createdat,
    transactions.updatedat
  FROM transactions
  LEFT JOIN accounts ON transactions.accountid = accounts.id
  LEFT JOIN logins ON accounts.loginid = logins.id
  WHERE
    -- ignore incoming transactions
    amount < 0
  AND
    (extra ->> 'additional' is null or (
        -- ignore MoneyFarm investments
        extra ->> 'additional' not like '%mfm investment ltd italian branch%'
      AND
        -- ignore Soisy investments
        extra ->> 'additional' not like '%soisy spa%'
      AND
        -- ignore Degiro investments
        extra ->> 'additional' not like '%degiro%'
      AND
        -- ignore Amex payments
        extra ->> 'additional' not like '%american express services%'
      AND
        -- ignore wire transfers to Irina
        extra ->> 'additional' not like '%bonifico da voi disposto a favore di: irina%'
    ))
  AND
    -- ignore Fineco credit card
    description not like '%CARTA DI CREDITO DI FINECOBANK%'
  AND
    -- ignore taxes
    description not like '%Pagamento deleghe f24%'
  AND
    -- ignore MAV payments
    description not like '%Pagamento mav%'
  AND
    -- ignore downpayment new house
    description != 'Assegno n. 8345921901'
  AND
    -- ignore cash withdrawals for new house
    NOT (description like '%Prelievo%' AND amount = -1000)
  AND
    -- ignore withdrawals from PayPal
    NOT (logins.providername = 'PayPal' AND description = 'Bank Account');
