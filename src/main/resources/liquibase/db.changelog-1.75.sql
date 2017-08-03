--liquibase formatted sql
--changeset luongnv:1.75
ALTER TABLE policy_jobs ADD COLUMN priority bigint(20);