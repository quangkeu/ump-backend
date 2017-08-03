--liquibase formatted sql
--changeset hieuph:1.9
create table firmware_tasks (
  id bigint auto_increment,
  group_name text,
  group_id bigint,
  type tinyint, -- 0 = now , 1== new contact
  created bigint,
  updated bigint,
  primary key(id)
);