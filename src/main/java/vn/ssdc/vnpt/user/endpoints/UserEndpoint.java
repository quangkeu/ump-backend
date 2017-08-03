package vn.ssdc.vnpt.user.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("user")
@Api("User")
@Produces(APPLICATION_JSON)
public class UserEndpoint extends SsdcCrudEndpoint<Long, User> {
    @Context
    private HttpServletRequest request;

    private UserService userService;

    @Autowired
    public UserEndpoint(UserService userService) {
        this.service = this.userService = userService;
    }

    @GET
    @Path("/get-page")
    public Page<User> getPage(@DefaultValue("0") @QueryParam("page") int page,
                              @DefaultValue("20") @QueryParam("limit") int limit,
                              @DefaultValue("") @QueryParam("where") String where
                              ) {
        String accessToken = request.getHeader("Authorization");
        return userService.getPage(page, limit, where);
    }

    @POST
    @Path("/forgot-password")
    public Boolean forgotPassword(Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            userService.sendForgotPassword(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @POST
    @Path("/change-password-with-token")
    public Boolean changePasswordWithToken(Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.containsKey("userId") ? request.get("userId") : "");
            String token = request.containsKey("token") ? request.get("token") : "";
            String newPassword = request.containsKey("newPassword") ? request.get("newPassword") : "";
            return userService.changePasswordWithToken(userId, token, newPassword);
        } catch (Exception e) {
            return false;
        }
    }

    @POST
    @Path(("/{email}/forgot-password-with-email"))
    public Boolean forgotPassword(@PathParam("email") String email) {
        userService.sendForgotPasswordWithEmail(email);
        return true;
    }

    @GET
    @Path("/get-by-username/{username}")
    public User resetPassword(@PathParam("username") String username) {
        return userService.findByUserName(username);
    }

    @POST
    @Path("/{userId}/reset-password")
    public User resetPassword(@PathParam("userId") Long id) {
        return userService.resetPassword(id);
    }

    @POST
    @Path("/check-current-password")
    public Boolean checkCurrentPassword(Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            String currentPassword = request.containsKey("currentPassword") ? request.get("currentPassword") : "";

            if (!username.isEmpty() && !currentPassword.isEmpty()) {
                return userService.checkPassword(username, currentPassword);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @POST
    @Path("/change-password")
    public User changePassword(Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            String newPassword = request.containsKey("newPassword") ? request.get("newPassword") : "";
            String currentPassword = request.containsKey("currentPassword") ? request.get("currentPassword") : "";

            return userService.changePassword(username, currentPassword, newPassword);
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    @Path("/check-by-role-id/{roleId}")
    public List<User> checkByRoleId(@PathParam("roleId") String roleId) {
        return userService.checkByRoleId(roleId);
    }

    @GET
    @Path("/get-user-by-group")
    public List<User> findUserByDeviceGroup(@QueryParam("groupId") String groupId) {
        return userService.getListUserByDeviceGroupId(groupId);
    }

}