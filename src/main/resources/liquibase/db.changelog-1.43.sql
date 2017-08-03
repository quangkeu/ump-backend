--liquibase formatted sql
--changeset thanhlx:1.43
ALTER TABLE `policy_task` RENAME `policy_tasks`;
ALTER TABLE `policy_tasks` CHANGE `policy_jobs_id` `policy_job_id` BIGINT;
ALTER TABLE `policy_tasks` CHANGE `status` `status` TINYINT(2);
ALTER TABLE `policy_tasks` CHANGE `done` `done` BIGINT;
ALTER TABLE `policy_tasks` CHANGE `task_id` `task_id` TEXT;