--liquibase formatted sql
--changeset thanhlx:1.28
ALTER TABLE device_types ADD model_name text;
ALTER TABLE device_type_versions ADD oui text;
ALTER TABLE device_type_versions ADD product_class text;
ALTER TABLE device_type_versions ADD model_name text;
ALTER TABLE device_type_versions DROP name;