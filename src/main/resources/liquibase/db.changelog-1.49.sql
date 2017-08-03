--liquibase formatted sql
--changeset thanhlx:1.49
CREATE TABLE operations (
    id varchar(128) PRIMARY KEY,
    name varchar(256),
    group_name varchar(256),
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);
ALTER TABLE permissions DROP PRIMARY KEY;
ALTER TABLE permissions DROP COLUMN id;
ALTER TABLE permissions ADD id BIGINT(20) NOT NULL AUTO_INCREMENT PRIMARY KEY;
ALTER TABLE permissions ADD COLUMN operation_ids VARCHAR(2048);
ALTER TABLE users ADD COLUMN operation_ids VARCHAR(2048);





