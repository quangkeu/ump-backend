--liquibase formatted sql
--changeset luongnv:1.52
INSERT INTO email_templates VALUES ('user.forgotpassword', '<p>Please click <a href=\"http://ump-dev/changeForgotPassword?userId=%d&token=%s\">here</a> to recover your password</p>', 'Forgot Password', null, null);
INSERT INTO email_templates VALUES ('user.randomPassword', '<p>Login to UMP Application with<br>Username: %s <br>Password: %s</p>', 'Reset Password', null, null);
INSERT INTO email_templates VALUES ('user.resetPassword', '<p>Login to UMP Application with new password<br>Username: %s <br>Password: %s</p>', 'Reset Password', null, null);