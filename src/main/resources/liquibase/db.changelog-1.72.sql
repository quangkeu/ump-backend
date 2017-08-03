--liquibase formatted sql
--changeset thanhlx:1.72
ALTER TABLE alarm_types ADD COLUMN notification tinyint(1); -- 1 - real-time | 2 - Changes | 3 - Passive
ALTER TABLE alarm_types ADD COLUMN time_settings tinyint(4);