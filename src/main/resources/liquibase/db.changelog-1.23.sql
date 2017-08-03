--liquibase formatted sql
--changeset luongnv:1.23
ALTER TABLE tags ADD synchronize tinyint(1);
