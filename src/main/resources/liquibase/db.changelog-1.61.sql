--liquibase formatted sql
--changeset khanhmq:1.61
ALTER TABLE `roles` CHANGE `operation_ids` `operation_ids` TEXT;