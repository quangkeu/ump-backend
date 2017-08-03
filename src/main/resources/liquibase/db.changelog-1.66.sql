--liquibase formatted sql
--changeset luongnv:1.66
create table logging_users (
  id bigint auto_increment,
  session text,
  username text,
  time text,
  actions text,
  created bigint,
  updated bigint,
  primary key(id)
);