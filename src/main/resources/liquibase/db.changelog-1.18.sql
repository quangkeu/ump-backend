--liquibase formatted sql
--changeset kiendt:1.18
DROP TABLE IF EXISTS `parameter_details`;
CREATE TABLE `parameter_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_type_version_id` bigint(20) DEFAULT NULL,
  `path` varchar(200) DEFAULT NULL,
  `tr069_name` varchar(200) DEFAULT NULL,
  `short_name` varchar(200) DEFAULT NULL,
  `data_type` varchar(50) DEFAULT NULL,
  `default_value` varchar(200) DEFAULT NULL,
  `rule` varchar(200) DEFAULT NULL,
  `parent_object` varchar(200) DEFAULT NULL,
  `version` varchar(200) DEFAULT NULL,
  `description` text,
  `profile` text,
  `access` varchar(50) DEFAULT NULL,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
)

