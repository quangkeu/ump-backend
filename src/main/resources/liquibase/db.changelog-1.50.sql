--liquibase formatted sql
--changeset thanhlx:1.50
ALTER TABLE roles ADD COLUMN operation_ids VARCHAR(2048);