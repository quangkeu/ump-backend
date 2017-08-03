--liquibase formatted sql
--changeset vietnq:1.3
create table filters (
  id bigint auto_increment,
  name text,
  user_id text,
  query text,
  created bigint,
  updated bigint,
  primary key(id)
);