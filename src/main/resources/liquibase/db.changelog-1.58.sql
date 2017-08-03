--liquibase formatted sql
--changeset luongnv:1.58
ALTER TABLE policy_jobs ADD limited BIGINT(20);