--liquibase formatted sql
--changeset vanluong:1.4
create table parameter_details (
  id bigint identity primary key,
  device_type_version_id bigint,
  path longvarchar,
  tr069_name longvarchar,
  short_name longvarchar,
  data_type longvarchar,
  default_value longvarchar,
  rule longvarchar,
  parent_object longvarchar,
  version longvarchar,
  description longvarchar,
  profile longvarchar,
  access longvarchar,
  created bigint,
  updated bigint,
  tr069_parent_object longvarchar,
  instance tinyint,
);

CREATE TABLE users (
  id bigint identity primary key,
  user_name longvarchar,
  full_name longvarchar,
  email longvarchar,
  password longvarchar,
  role_ids longvarchar,
  role_names longvarchar,
  device_group_ids longvarchar,
  device_group_names longvarchar,
  avatar_url longvarchar,
  phone longvarchar,
  description longvarchar,
  forgot_pwd_token longvarchar,
  created bigint,
  updated bigint,
  forgot_pwd_token_requested bigint,
  operation_ids longvarchar
)

CREATE TABLE email_templates (
  id longvarchar,
  value longvarchar,
  description longvarchar,
  created bigint,
  updated bigint
)
--
-- INSERT INTO email_templates ('id', 'value', 'description', 'created', 'updated') VALUES ('user.forgotpassword', '<p>Please click <a href=\"http://ump-dev/changeForgotPassword?userId=%d&token=%s\">here</a> to recover your password</p>', 'Forgot Password', '', '');
-- -- INSERT INTO email_templates (id, value, description, created, updated) VALUES ('user.randomPassword', '<p>Login to UMP Application with<br>Username: %s <br>Password: %s</p>', 'Reset Password', '', NULL);
-- -- INSERT INTO email_templates (id, value, description, created, updated) VALUES ('user.resetPassword', '<p>Login to UMP Application with new password<br>Username: %s <br>Password: %s</p>', 'Reset Password', '', '');
