--liquibase formatted sql
--changeset thanhlx:1.35
ALTER TABLE parameter_details CHANGE default_value default_value text null;
