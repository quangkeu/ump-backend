--liquibase formatted sql
--changeset thanhlx:1.51
CREATE TABLE email_templates (
    id varchar(128) PRIMARY KEY,
    value TEXT,
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);