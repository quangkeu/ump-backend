--liquibase formatted sql
--changeset luongnv:1.11
create table subscriber_templates (
  id bigint auto_increment,
  name text ,
  template_keys mediumtext,
  created bigint,
  updated bigint,
  primary key(id)
);

