--liquibase formatted sql
--changeset thangnc:1.21
ALTER TABLE device_groups ADD oui TINYTEXT;
ALTER TABLE device_groups ADD product_class TINYTEXT;