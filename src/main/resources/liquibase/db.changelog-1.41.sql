--liquibase formatted sql
--changeset thanhlx:1.41
ALTER TABLE `policy_jobs` CHANGE id id bigint(20) AUTO_INCREMENT;
ALTER TABLE `policy_task` CHANGE id id bigint(20) AUTO_INCREMENT;