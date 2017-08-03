--liquibase formatted sql
--changeset thanhlx:1.44
ALTER TABLE `policy_jobs` CHANGE `preset_id` `preset_id` TEXT;