package vn.ssdc.vnpt.user.services;

import org.apache.commons.lang3.RandomStringUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.services.EmailTemplateService;
import vn.ssdc.vnpt.common.services.MailService;
import vn.ssdc.vnpt.umpexception.UserNotFoundException;
import vn.ssdc.vnpt.user.model.Role;
import vn.ssdc.vnpt.user.model.User;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService extends SsdcCrudService<Long, User> {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public MailService mailService;

    @Autowired
    public RoleService roleService;

    @Autowired
    public EmailTemplateService emailTemplateService;

    @Autowired
    public UserService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(User.class);
    }

    public Page<User> getPage(int page, int limit, String where) {
        return this.repository.search(where, new PageRequest(page, limit));
    }

    public User findByUserName(String username){
        List<User> userList = this.repository.search("user_name = ?",username);
        if(userList.isEmpty()){
            throw new EntityNotFoundException("users",username);
        }
        return userList.get(0);
    }

    public void sendForgotPassword(String username) throws UserNotFoundException {
        User user = findByUserName(username);
        if(user == null) {
            throw new UserNotFoundException("No user with username " + username);
        }
        user.forgotPwdToken = UUID.randomUUID().toString();
        user.forgotPwdTokenRequested = System.currentTimeMillis();
        update(user.id, user);

        //send email forgot password template: <p>Please click <a href="http://ump-dev/changeForgotPassword?userId=%d&token=%s">here</a> to recover your password</p>
        String mailContent = String.format(emailTemplateService.get("user.forgotpassword").value, user.id, user.forgotPwdToken, user.forgotPwdToken);
        mailService.sendMail(user.email,"Forgot Password",mailContent,null,null);
    }

    public void sendForgotPasswordWithEmail(String email) throws UserNotFoundException {
        List<User> users = getAll();
        User user = null;
        for (User u: users) {
            if(email.toLowerCase().equals(u.email.toLowerCase())) {
                user = u;
                break;
            }
        }
        if(user == null) {
            throw new UserNotFoundException("No user with email " + email);
        }
        user.forgotPwdToken = UUID.randomUUID().toString();
        user.forgotPwdTokenRequested = System.currentTimeMillis();
        update(user.id, user);

        //send email forgot password template: <p>Please click <a href="http://ump-dev/changeForgotPassword?userId=%d&token=%s">here</a> to recover your password</p>
        String mailContent = String.format(emailTemplateService.get("user.forgotpassword").value, user.id, user.forgotPwdToken, user.forgotPwdToken);
        mailService.sendMail(user.email,"Forgot Password",mailContent,null,null);
    }

    private void sendRandomPasswordEmail(User user) throws UserNotFoundException {
        String randomPassword = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        user.setEncryptedPassword(randomPassword);

        String mailContent = String.format(emailTemplateService.get("user.randomPassword").value, user.userName, randomPassword);
        mailService.sendMail(user.email, "Random Password", mailContent, null, null);
    }

    @Override
    public void beforeCreate(User user) {
        user = updateOperationIds(user);
        sendRandomPasswordEmail(user);
        super.beforeCreate(user);
    }

    @Override
    public void beforeUpdate(Long id, User user) {
        User userOld = get(id);
        // Send email to new email
        if(user.email!=null && !userOld.email.equals(user.email)) {
            sendRandomPasswordEmail(user);
        }
        user = updateOperationIds(user);
        super.beforeUpdate(id, user);
    }

    public User resetPassword(Long id) {
        User user = get(id);
        if(user == null) {
            throw new UserNotFoundException("No user with id " + id);
        }

        String randomPassword = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        user.setEncryptedPassword(randomPassword);

        update(id, user);

        String mailContent = String.format(emailTemplateService.get("user.resetPassword").value, user.userName, randomPassword);
        mailService.sendMail(user.email, "Reset Password", mailContent, null, null);

        return user;
    }

    public Boolean checkPassword(String username, String currentPassword) {
        User user = findByUserName(username);
        return BCrypt.checkpw(currentPassword, user.password);
    }

    public User changePassword(String username, String currentPassword, String newPassword) {
        if(checkPassword(username, currentPassword)) {
            User user = findByUserName(username);
            user.setEncryptedPassword(newPassword);
            update(user.id, user);
            return user;
        } else {
            return null;
        }
    }

    public Boolean checkToken(Long id, String token) {
        User user = get(id);
        return token.equals(user.forgotPwdToken);
    }

    public Boolean changePasswordWithToken(Long userId, String token, String newPassword) {
        if(checkToken(userId, token)) {
            User user = get(userId);
            if(user != null) {
                user.setEncryptedPassword(newPassword);
                user.forgotPwdToken = null;
                user.forgotPwdTokenRequested = null;
                update(userId, user);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean getByName(String name) {
        int count = this.repository.search("user_name=?", name).size();
        if (count == 0) {
            return false;
        }
        return true;
    }

    private User updateOperationIds(User user) {
        Set<String> operationIds = new HashSet<>();
        for (String roleId : user.roleIds) {
            Role role = roleService.get(Long.valueOf(roleId));
            if(role != null) {
                operationIds.addAll(role.operationIds);
            }
        }
        user.operationIds = operationIds;

        return user;
    }

    public List<User> checkByRoleId(String roleId) {
        return this.repository.search("role_ids LIKE '%"+roleId+"%'");
    }
    public List<User> getListUserByDeviceGroupId(String deviceGroupId) {
        return this.repository.search("device_group_ids LIKE '%\""+deviceGroupId+"\"%'");
    }
}