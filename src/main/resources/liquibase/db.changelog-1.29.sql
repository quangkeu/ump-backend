--liquibase formatted sql
--changeset luongnv:1.29
ALTER TABLE subscribers
CHANGE subscriber_data_template_id subscriber_data_template_ids text