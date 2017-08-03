--liquibase formatted sql
--changeset khanhmq:1.60
ALTER TABLE `permissions` CHANGE `operation_ids` `operation_ids` TEXT;