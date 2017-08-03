--liquibase formatted sql
--changeset luongnv:1.32
ALTER TABLE subscriber_devices DROP INDEX device_id;