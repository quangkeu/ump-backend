--liquibase formatted sql
--changeset khanhmq:1.33
create table blacklist_devices (
  id bigint auto_increment,
  device_id text,
  created bigint,
  updated bigint,
  primary key(id)
);