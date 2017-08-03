--liquibase formatted sql
--changeset thangnc:1.19
DROP TABLE IF EXISTS `device_groups`;
CREATE TABLE `device_groups` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` text,
  `filters` text,
  `query` text,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

