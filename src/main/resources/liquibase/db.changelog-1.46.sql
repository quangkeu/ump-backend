--liquibase formatted sql
--changeset thanhlx:1.46
ALTER TABLE `parameter_details` CHANGE `access` `access` VARCHAR(16);
ALTER TABLE `tr069_parameters` CHANGE `access` `access` VARCHAR(16);
