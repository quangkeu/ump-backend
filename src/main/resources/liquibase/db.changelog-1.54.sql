--liquibase formatted sql
--changeset thanhlx:1.54
ALTER TABLE alarms ADD COLUMN severity varchar(32);