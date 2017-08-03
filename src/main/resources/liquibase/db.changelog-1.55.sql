--liquibase formatted sql
--changeset thanhlx:1.55
ALTER TABLE alarms ADD COLUMN alarm_type varchar(1024);