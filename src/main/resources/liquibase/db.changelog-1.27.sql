--liquibase formatted sql
--changeset tuanha2:1.27
RENAME TABLE diagnostics_model TO diagnostics_task;
ALTER TABLE diagnostics_task CHANGE device_type_id device_id text;
ALTER TABLE diagnostics_task CHANGE done done bigint(20);
