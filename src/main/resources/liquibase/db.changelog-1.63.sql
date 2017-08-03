--liquibase formatted sql
--changeset thanhlx:1.63
CREATE TABLE configurations (
    id varchar(128) PRIMARY KEY,
    value TEXT,
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);