--liquibase formatted sql
--changeset hieuph:1.16
alter table tags add root_tag_id bigint;

