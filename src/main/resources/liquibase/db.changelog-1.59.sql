--liquibase formatted sql
--changeset luongnv:1.59
ALTER TABLE device_groups ADD COLUMN oui varchar(256);
ALTER TABLE device_groups ADD COLUMN product_class varchar(256);