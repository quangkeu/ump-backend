--liquibase formatted sql
--changeset thanhlx:1.69
CREATE TABLE performance_settings (
    id bigint(20) AUTO_INCREMENT,
    stastics_type varchar(64), -- LAN | WAN | WLAN | RAM | VOIP | STB
    type varchar(64), -- RECEIVED | TRANSMITTED
    device_id varchar(1024),
    device_group_id bigint(20),
    external_devices longtext,
    external_filename varchar(1024),
    start bigint(20),
    end bigint(20),
    created bigint,
	updated bigint,
	primary key(id)
);