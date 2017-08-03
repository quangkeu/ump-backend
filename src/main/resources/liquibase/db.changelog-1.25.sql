--liquibase formatted sql
--changeset tuanha2:1.25
ALTER TABLE tr069_profiles ADD diagnostics tinyint(1);
