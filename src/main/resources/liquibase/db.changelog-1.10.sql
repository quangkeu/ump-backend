--liquibase formatted sql
--changeset hieuph:1.10
create table subscribers (
  id bigint auto_increment,
  subscriber_id text ,
  subscriber_data_template_id bigint ,
  subscriber_data mediumtext,
  created bigint,
  updated bigint,
  primary key(id)
);