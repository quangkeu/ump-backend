package vn.ssdc.vnpt.user.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.user.services.RoleService;
import vn.ssdc.vnpt.user.model.Role;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Lamborgini on 5/4/2017.
 */
@Component
@Path("role")
@Api("Role")
@Produces(APPLICATION_JSON)
public class RoleEndPoint extends SsdcCrudEndpoint<Long, Role> {

    private RoleService roleService;

    @Autowired
    public RoleEndPoint(RoleService roleService) {
        this.service = this.roleService = roleService;
    }

    @GET
    @Path("/search-role")
    public List<Role> searchRole(
            @DefaultValue("20") @QueryParam("limit") String limit,
            @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        return this.roleService.searchRole(limit,indexPage);
    }

    @GET
    @Path("/check-role-name")
    public int checkName(@DefaultValue("") @QueryParam("addNameRole") String addName) {
        return this.roleService.checkName(addName);
    }

    @GET
    @Path("/check-by-permission-id/{permissionId}")
    public List<Role> checkByPermissionId(@PathParam("permissionId") String permissionId) {
        return this.roleService.checkByPermissionId(permissionId);
    }

    @GET
    @Path("/get-children-roles")
    public List<Role> getChildrenRoles(@DefaultValue("") @QueryParam("username") String username) {
        if (!"".equals(username)) {
            return this.roleService.getListChildren(username);
        } else {
            return null;
        }
    }
}
