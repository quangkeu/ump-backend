--liquibase formatted sql
--changeset tuanha2:1.6
ALTER TABLE alarm_details ADD COLUMN device_groups longvarchar;
ALTER TABLE alarms ADD COLUMN device_groups longvarchar;


