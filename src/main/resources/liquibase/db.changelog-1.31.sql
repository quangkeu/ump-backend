--liquibase formatted sql
--changeset thanhlx:1.31
RENAME TABLE diagnostics_task TO diagnostic_tasks;