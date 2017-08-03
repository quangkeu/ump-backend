--liquibase formatted sql
--changeset thanhlx:1.48
CREATE TABLE users (
	 id bigint(20) AUTO_INCREMENT PRIMARY KEY,
	 user_name varchar(32),
	 full_name varchar(64),
	 email varchar(64),
	 password varchar(256),
	 role_ids varchar(256),
	 role_names varchar(1024),
     device_group_ids varchar(256),
     device_group_names varchar(1024),
     avatar_url varchar(1024),
     phone varchar(16),
     description varchar(1024),
     forgot_pwd_token varchar(256),
	 created bigint(20),
	 updated bigint(20),
	 forgot_pwd_token_requested bigint(20)
);
CREATE TABLE permissions (
    id varchar(128) PRIMARY KEY,
    name varchar(256),
    group_name varchar(256),
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);
CREATE TABLE roles (
	id bigint(20) AUTO_INCREMENT PRIMARY KEY,
    name varchar(256),
    permissions_ids varchar(256),
    description varchar(1024),
    created bigint(20),
	updated bigint(20)
);





