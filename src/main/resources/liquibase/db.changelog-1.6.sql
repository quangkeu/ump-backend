--liquibase formatted sql
--changeset vietnq:1.6
alter table tags add assigned_group int;
alter table tags add parent_object text;



