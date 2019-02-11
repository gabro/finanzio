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
);

create table if not exists accounts (
    id varchar(80) primary key,
    name varchar(80) not null,
    nature varchar(80) not null,
    balance real not null,
    currencyCode varchar(80) not null,
    loginId varchar(80) not null,
    createdAt timestamp not null,
    updatedAt timestamp not null
);

create table if not exists logins (
    id varchar(80) primary key,
    providerName varchar(80) not null
);

create table if not exists splitwise_users (
    id bigint primary key,
    firstName varchar(80),
    lastName varchar(80)
);

create table if not exists splitwise_expense_shares (
    expenseId bigint,
    userId bigint references splitwise_users(id),
    paidShare real not null,
    owedShare real not null,
    netBalance real not null,
    primary key (expenseId, userId)
);

create table if not exists splitwise_expenses (
    id bigint primary key,
    groupId bigint,
    description text not null,
    repeats boolean not null,
    repeatInterval varchar(80),
    details text,
    cost real not null,
    currencyCode varchar(5) not null,
    date timestamp not null,
    createdBy bigint not null references splitwise_users(id),
    payment boolean not null
);
