--liquibase formatted sql
--changeset khanhmq:1.65
ALTER TABLE `alarms` MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;