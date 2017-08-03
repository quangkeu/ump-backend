--liquibase formatted sql
--changeset thanhlx:1.38
ALTER TABLE parameter_details ADD instance tinyint(1);
