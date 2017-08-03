--liquibase formatted sql
--changeset hieuph:1.8
create table area_groups (
  id bigint auto_increment,
  name text,
  parent_id bigint,
  type text,
  ip_address_range text,
  created bigint,
  updated bigint,
  primary key(id)
);