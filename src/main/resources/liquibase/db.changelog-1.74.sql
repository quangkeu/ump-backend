--liquibase formatted sql
--changeset tuanha2:1.74
INSERT INTO email_templates VALUES ('alarm.notify_ver2','<p>ALARM DEVICE OVER AGGREGATED VOLUME %s <br>  OVER AGGREGATED VOLUME <br> ERROR TYPE : %s <br> ERROR NAME : %s </p>', 'Alarm Notification',null,null);