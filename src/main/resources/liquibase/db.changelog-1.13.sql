--liquibase formatted sql
--changeset luongnv:1.13
alter table subscribers ADD UNIQUE(subscriber_id(255));