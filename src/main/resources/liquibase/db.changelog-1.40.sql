--liquibase formatted sql
--changeset thangnc:1.40
ALTER TABLE `device_groups` ADD firmware_version TINYTEXT;
ALTER TABLE `device_groups` ADD label VARCHAR(255);
ALTER TABLE `device_groups` CHANGE oui manufacturer TINYTEXT;
ALTER TABLE `device_groups` CHANGE product_class model_name TINYTEXT;
