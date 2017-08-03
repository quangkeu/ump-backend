--liquibase formatted sql
--changeset thanhlx:1.57
ALTER TABLE alarm_types DROP COLUMN id;
ALTER TABLE alarm_types ADD id BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY;