--liquibase formatted sql
--changeset luongnv:1.30
ALTER TABLE subscriber_devices
CHANGE device_id device_id text null;
