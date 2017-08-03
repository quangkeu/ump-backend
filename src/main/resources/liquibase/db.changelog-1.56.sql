--liquibase formatted sql
--changeset thanhlx:1.56
ALTER TABLE alarms DROP COLUMN alarm_type;
ALTER TABLE alarms ADD COLUMN alarm_name varchar(1024);