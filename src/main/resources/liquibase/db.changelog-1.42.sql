--liquibase formatted sql
--changeset luongnv:1.42
ALTER TABLE `policy_jobs` ADD name VARCHAR(255);