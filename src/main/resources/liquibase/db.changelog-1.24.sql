--liquibase formatted sql
--changeset tuanha2:1.24
create table diagnostics_model (
  id bigint auto_increment PRIMARY KEY,
  device_type_id bigint ,
  diagnostics_name text,
  parameter_full mediumtext,
  request mediumtext,
  result text,
  status text,
  done text,
  task_id text,
  created bigint,
  updated bigint
);