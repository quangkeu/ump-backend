--liquibase formatted sql
--changeset hangtt:1.7
alter table device_types add interval_time bigint;