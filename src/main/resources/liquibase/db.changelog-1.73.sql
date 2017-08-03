--liquibase formatted sql
--changeset tuanha2:1.73
INSERT INTO email_templates VALUES ('alarm.notify','<p>ALARM DEVICE OVER AGGREGATED VOLUME %s <br> DEVICE NAME : %s OVER AGGREGATED VOLUME <br> ERROR TYPE : %s <br> ERROR NAME : %s </p>', 'Alarm Notification',null,null);