create table if not exists transactions (
    id varchar(80) primary key,
    mode varchar(80) not null,
    status varchar(80) not null,
    madeOn timestamp not null,
    amount real not null,
    currencyCode varchar(80) not null,
    description text not null,
    category text not null,
    duplicated boolean not null,
    extra json not null,
    accountId varchar(80) not null,
    createdAt timestamp not null,
    updatedAt timestamp not null
)
