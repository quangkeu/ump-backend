--liquibase formatted sql
--changeset khanhmq:1.5
CREATE TABLE alarms (
    id bigint identity primary key,
    device_id longvarchar,
    alarm_type_id bigint,
    alarm_type_name longvarchar,
    device_group_id bigint,
    device_group_name longvarchar,
    raised bigint,
    status longvarchar,
    description longvarchar,
    severity longvarchar,
    alarm_name longvarchar,
    created bigint,
	updated bigint
);

CREATE TABLE operations (
    id bigint identity primary key,
    name longvarchar,
    group_name longvarchar,
    description longvarchar,
    created bigint,
	updated bigint
);

CREATE TABLE permissions (
    id bigint primary key,
    name longvarchar,
    group_name longvarchar,
    description longvarchar,
    operation_ids longvarchar,
    created bigint,
	updated bigint
);
CREATE TABLE roles (
	id bigint identity primary key,
    name longvarchar,
    permissions_ids longvarchar,
    description longvarchar,
    operation_ids longvarchar,
    created bigint,
	updated bigint
);