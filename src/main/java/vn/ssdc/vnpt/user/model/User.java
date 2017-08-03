package vn.ssdc.vnpt.user.model;

import org.mindrot.jbcrypt.BCrypt;
import vn.ssdc.vnpt.umpexception.ForgotPasswordTokenExpiredException;
import vn.ssdc.vnpt.umpexception.InvalidForgotPasswordTokenException;
import vn.ssdc.vnpt.umpexception.InvalidRequestException;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.LinkedHashSet;
import java.util.Set;

import static vn.ssdc.vnpt.user.utils.Constants.FORGOT_PASSWORD_EXPIRED_TIME;

public class User extends SsdcEntity<Long> {

    public String userName;
    public String fullName;
    public String email;
    public String password;
    public Set<String> roleIds;
    public Set<String> roleNames;
    public Set<String> deviceGroupIds;
    public Set<String> deviceGroupNames;
    public Set<String> operationIds;
    public String avatarUrl;
    public String phone;
    public String description;
    public String forgotPwdToken;
    public Long forgotPwdTokenRequested;

    public User() {
        this.roleIds = new LinkedHashSet<String>();
        this.roleNames = new LinkedHashSet<String>();
        this.deviceGroupIds = new LinkedHashSet<String>();
        this.deviceGroupNames = new LinkedHashSet<String>();
        this.operationIds = new LinkedHashSet<String>();
    }

    private Boolean forgotTokenStillValid() {
        if(forgotPwdToken == null && forgotPwdTokenRequested == null) {
            return false;
        }
        return System.currentTimeMillis() - forgotPwdTokenRequested < FORGOT_PASSWORD_EXPIRED_TIME;
    }

    public void clearForgottenPasswordToken() {
        forgotPwdToken = null;
        forgotPwdTokenRequested = null;
    }

    public Boolean changeForgottenPassword(String newPassword, String token) throws InvalidForgotPasswordTokenException, ForgotPasswordTokenExpiredException {
        if(token == null || !token.equals(forgotPwdToken)) {
            throw new InvalidForgotPasswordTokenException("Invalid token");
        }
        if(forgotTokenStillValid()) {
            password = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        } else {
            throw new ForgotPasswordTokenExpiredException("Token is expired");
        }
        return true;
    }

    public void changePassword(String currentPassword, String newPassword) throws InvalidRequestException {
        if(!BCrypt.checkpw(currentPassword,this.password)) {
            throw new InvalidRequestException("Current password is not correct");
        }
        setEncryptedPassword(newPassword);
    }

    public void setEncryptedPassword(String password) {
        this.password = encryptedPassword(password);
    }

    private String encryptedPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}