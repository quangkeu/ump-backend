--liquibase formatted sql
--changeset thanhlx:1.70
ALTER TABLE performance_settings ADD COLUMN stastics_interval tinyint(4);
ALTER TABLE performance_settings ADD COLUMN monitoring tinyint(1); -- 1 - Single CPE | 2 - By group filter | 3 - By external file
ALTER TABLE performance_settings ADD COLUMN manufacturer varchar(64);
ALTER TABLE performance_settings ADD COLUMN model_name varchar(64);
ALTER TABLE performance_settings ADD COLUMN serial_number varchar(64);