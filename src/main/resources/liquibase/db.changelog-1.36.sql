--liquibase formatted sql
--changeset luongnv:1.36
ALTER TABLE parameter_details ADD tr069_parent_object varchar(255);