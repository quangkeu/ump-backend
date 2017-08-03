--liquibase formatted sql
--changeset thanhlx:1.39
ALTER TABLE policy_jobs CHANGE events events text null;