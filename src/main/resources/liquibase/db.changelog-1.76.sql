--liquibase formatted sql
--changeset tuanha:1.76
ALTER TABLE alarms ADD COLUMN device_groups text;
ALTER TABLE alarm_details ADD COLUMN device_groups text;