--liquibase formatted sql
--changeset thangnc:1.3
create table device_groups (
  id bigint identity primary key,
  name longvarchar,
  filters longvarchar,
  query longvarchar,
  created bigint,
  updated bigint,
  manufacturer longvarchar,
  model_name longvarchar,
  firmware_version longvarchar,
  label longvarchar,
  oui longvarchar,
  product_class longvarchar,
);