--liquibase formatted sql
--changeset vietnq:1.5
alter table device_types drop column data_model_file_name;
alter table device_types drop column firmware_version;
alter table device_types drop column parameters;

alter table tags drop column device_type_id;
alter table tags add device_type_version_id bigint;

create table device_type_versions (
  id bigint auto_increment,
  device_type_id bigint,
  firmware_version tinytext,
  data_model_file_name text,
  firmware_file_name text,
  firmware_file_id text,
  parameters mediumtext,
  created bigint,
  updated bigint,
  primary key(id)
);

