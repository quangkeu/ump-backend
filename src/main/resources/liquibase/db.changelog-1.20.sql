--liquibase formatted sql
--changeset kiendt:1.20
DROP TABLE IF EXISTS `tr069_parameters`;
CREATE TABLE `tr069_parameters` (
  `path` varchar(200) NOT NULL,
  `data_type` varchar(50) DEFAULT NULL,
  `default_value` text,
  `rule` text,
  `parent_object` varchar(200) DEFAULT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` text,
  `access` varchar(50) DEFAULT NULL,
  `other_attributes` text,
  `profile_names` text,
  `id` bigint(20) DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`path`)
);

DROP TABLE IF EXISTS `tr069_profiles`;
CREATE TABLE `tr069_profiles` (
  `name` varchar(200) NOT NULL,
  `version` varchar(200) DEFAULT NULL,
  `parameters` longtext,
  `id` bigint(20) DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`name`)
);
