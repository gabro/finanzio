create table if not exists transactions (
    id varchar(80) primary key,
    mode varchar(80),
    status varchar(80),
    madeOn varchar(80),
    amount real,
    currencyCode varchar(80),
    description text,
    category text,
    duplicated boolean,
    accountId varchar(80),
    createdAt timestamp,
    updatedAt timestamp
)
