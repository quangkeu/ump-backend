--liquibase formatted sql
--changeset thanhlx:1.53
CREATE TABLE alarm_types (
    id bigint(20) PRIMARY KEY,
    type varchar(64), -- REQUEST_FAIL | CONFIGURATION_FAIL | UPDATE_FIRMWARE_FAIL | REBOOT_FAIL | FACTORY_RESET_FAIL | PARAMETER_VALUE
    name varchar(1024),
    device_groups text, -- Set DeviceGroup
    severity varchar(32), -- INFO | WARNING | MAJOR | MINOR | CRITICAL
    notify tinyint(1),
    aggregated_volume bigint(20),
    notify_aggregated varchar(32), -- OFF | SMS | EMAIL
    parameter_values text,
    created bigint(20),
	updated bigint(20)
);
CREATE TABLE alarms (
    id bigint(20) PRIMARY KEY,
    device_id varchar(1024),
    alarm_type_id bigint(20),
    alarm_type_name varchar(1024),
    device_group_id bigint(20),
    device_group_name varchar(1024),
    raised bigint(20),
    status varchar(32), -- ACTIVE | CLEARED
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);