--liquibase formatted sql
--changeset vietnq:1.2
create table device_types (
  id bigint auto_increment,
  name text,
  firmware_version tinytext,
  product_class tinytext,
  manufacturer text,
  oui tinytext,
  parameters mediumtext,
  created bigint,
  updated bigint,
  primary key(id)
);

create table tags (
  id bigint auto_increment,
  created bigint,
  updated bigint,
  name tinytext,
  parameters mediumtext,
  assigned tinyint,
  type tinytext,
  device_type_id bigint,
  primary key(id)
);