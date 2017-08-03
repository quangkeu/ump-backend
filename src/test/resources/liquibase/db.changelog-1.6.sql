--liquibase formatted sql
--changeset tuanha2:1.6
CREATE TABLE alarm_details (
    id bigint identity primary key,
    alarm_type_id bigint,
    alarm_type longvarchar,
    alarm_type_name longvarchar,
    device_id longvarchar,
    created bigint,
	updated bigint
);
