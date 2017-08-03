--liquibase formatted sql
--changeset kiendt:1.14
DROP TABLE IF EXISTS `provisionings`;
CREATE TABLE `provisionings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_id` bigint(20) DEFAULT NULL,
  `parent_object` varchar(255) DEFAULT NULL,
  `parameter` mediumtext,
  `created` bigint(20) DEFAULT NULL,
  `updated` bigint(20) DEFAULT NULL,
  `preset_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)