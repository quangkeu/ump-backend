--liquibase formatted sql
--changeset tuanha:1.37
CREATE TABLE policy_jobs (
	 id bigint(20) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	 status text,
	 device_group_id bigint(20),
	 external_devices longText,
	 external_filename text,
	 start_at 	bigint(20),
	 time_interval int,
	 max_number int,
	 events int,
	 is_immediately int,
	 action_name text,
	 parameters longText,
	 preset_id int,
	 created bigint(20),
	 updated bigint(20)
	 );
CREATE TABLE policy_task(
	id bigint(20) UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    created bigint(20),
    updated bigint(20),
    device_id text,
    policy_jobs_id bigint(20),
    status text,
    done text,
    task_id bigint(20)
);
