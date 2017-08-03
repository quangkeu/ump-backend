--liquibase formatted sql
--changeset thanhlx:1.47
ALTER TABLE `policy_tasks` ADD COLUMN `error_code` VARCHAR(8);
ALTER TABLE `policy_tasks` ADD COLUMN `error_text` VARCHAR(1024);
