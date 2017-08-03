--liquibase formatted sql
--changeset hieuph:1.12
create table subscriber_devices(
  id bigint auto_increment,
  subscriber_id text ,
  device_id text not null,
  manufacturer text,
  oui text,
  product_class text,
  serial_number text,
  created bigint,
  updated bigint,
  primary key(id)
);

alter table subscriber_devices ADD UNIQUE(device_id(255));