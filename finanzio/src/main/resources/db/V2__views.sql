CREATE OR REPLACE VIEW transactions_metabase AS
 SELECT
    transactions.id,
    transactions.madeon,
    - transactions.amount as amount,
    COALESCE(splitwise_expense_shares.owedshare, - transactions.amount) as owed_amount,
    transactions.description,
    transactions.category,
    transactions.duplicated,
    transactions.accountid,
    logins.providername as login_name,
    accounts.name as account_name,
    accounts.nature as account_nature,
    splitwise_expenses.description as splitwise_description,
    splitwise_expense_shares.owedshare as splitwise_owed_share
   FROM transactions
     LEFT JOIN accounts ON transactions.accountid = accounts.id
     LEFT JOIN logins ON accounts.loginid = logins.id
     LEFT JOIN splitwise_matched_transactions ON transactions.id = splitwise_matched_transactions.saltedge_transaction_id
     LEFT JOIN splitwise_expense_shares ON splitwise_matched_transactions.splitwise_expense_id = splitwise_expense_shares.expenseid AND splitwise_matched_transactions.splitwise_user_id = splitwise_expense_shares.userid
     LEFT JOIN splitwise_expenses ON splitwise_expenses.id = splitwise_matched_transactions.splitwise_expense_id
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
    transactions.description not like '%CARTA DI CREDITO DI FINECOBANK%'
  AND
    -- ignore taxes
    transactions.description not like '%Pagamento deleghe f24%'
  AND
    -- ignore MAV payments
    transactions.description not like '%Pagamento mav%'
  AND
    -- ignore downpayment new house
    NOT (transactions.description = 'Assegno n. 8345921901' OR
         transactions.description = 'Assegno n. 8345921902'
        )
  AND
    -- ignore cash withdrawals for new house
    NOT (transactions.description like '%Prelevamento carta N° ***** 767%' AND amount = -250)
  AND
    -- ignore cash withdrawals for new house
    NOT (transactions.description like '%Prelievo%' AND amount = -1000)
  AND
    -- ignore withdrawals from PayPal
    NOT (logins.providername = 'PayPal' AND transactions.description = 'Bank Account')
  AND
    -- ignore matched betting stuff
    NOT (logins.providername = 'PayPal' AND (
        transactions.description = 'Betflag S.p.A.' OR
        transactions.description = 'Betclic Limited' OR
        transactions.description = 'Betfair Italia SRL' OR
        transactions.description = 'Lottomatica Scommesse Srl' OR
        transactions.description = 'Sisal' OR
        transactions.description = 'Stars'
      ))
  AND
    -- ignore BPER mortgage stuff
    NOT (logins.providername = 'BPER (SmartWeb)' AND (
      transactions.description like '%GIROCONTO%' OR
      transactions.description = 'TOTALE SPESE'
    ))

UNION

  -- splitwise expense
   SELECT id::varchar as id,
    date as madeon,
    cost as amount,
    owedshare as owed_amount,
    description,
    NULL as category,
    FALSE as duplicated,
    NULL as accountId,
    'Splitwise' as login_name,
    'Splitwise' as account_name,
    NULL as account_nature,
    description as splitwise_description,
    owedshare as splitwise_owed_share
FROM splitwise_expenses LEFT JOIN splitwise_expense_shares ON splitwise_expenses.id = splitwise_expense_shares.expenseid
WHERE splitwise_expense_shares.userid = '4393813' and splitwise_expense_shares.paidshare = 0;
