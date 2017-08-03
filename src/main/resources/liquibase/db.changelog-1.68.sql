--liquibase formatted sql
--changeset thanhlx:1.68
ALTER TABLE `users` CHANGE `operation_ids` `operation_ids` TEXT;