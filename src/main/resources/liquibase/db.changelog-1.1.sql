--liquibase formatted sql
--changeset vietnq:1.1
create table demos (
  id bigint auto_increment,
  name text,
  created bigint,
  updated bigint,
  primary key(id)
);