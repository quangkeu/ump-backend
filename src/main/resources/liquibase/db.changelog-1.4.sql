--liquibase formatted sql
--changeset luongnv:1.4
alter table device_types add data_model_file_name text;
alter table device_types add connection_request_username text;
alter table device_types add connection_request_password text;

