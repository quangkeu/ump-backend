--liquibase formatted sql
--changeset thanhlx:1.62
ALTER TABLE alarm_types ADD COLUMN monitor tinyint(1);
ALTER TABLE alarms DROP COLUMN device_group_id;
ALTER TABLE alarms DROP COLUMN device_group_name;
