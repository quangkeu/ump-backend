--liquibase formatted sql
--changeset tuanha2:1.64
CREATE TABLE alarm_details (
    id bigint auto_increment,
    alarm_type_id bigint,
    alarm_type text,
    alarm_type_name text,
    device_id text,
    created bigint,
	updated bigint,
	primary key(id)
);