--liquibase formatted sql
--changeset khanhmq:1.26
ALTER TABLE device_type_versions ADD manufacturer text;
ALTER TABLE device_type_versions ADD name text;


