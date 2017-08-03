--liquibase formatted sql
--changeset hieuph:1.15
alter table firmware_tasks add firmware_version_id bigint;

