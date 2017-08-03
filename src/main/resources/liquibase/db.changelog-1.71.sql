--liquibase formatted sql
--changeset thanhlx:1.71
ALTER TABLE performance_settings ADD COLUMN parameter_names LONGTEXT;
CREATE TABLE performances (
    id bigint(20) AUTO_INCREMENT,
    performance_setting_id bigint(20),
    parameter_names longtext,
    value_changes longtext,
    created bigint,
	updated bigint,
	primary key(id)
);