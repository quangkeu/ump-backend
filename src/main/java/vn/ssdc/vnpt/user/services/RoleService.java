package vn.ssdc.vnpt.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.user.model.Role;
import vn.ssdc.vnpt.user.model.User;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lamborgini on 5/4/2017.
 */
@Service
public class RoleService extends SsdcCrudService<Long, Role> {

    @Autowired
    public UserService userService;

    @Autowired
    public RoleService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Role.class);
    }

    public List<Role> searchRole(String limit, String indexPage) {
        Page<Role> all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit)));
        return  all.getContent();
    }

    public int checkName(String addName) {
        String whereExp = "name=?";
        return this.repository.search(whereExp, addName).size();
    }

    public List<Role> checkByPermissionId(String permissionId) {
        return this.repository.search("permissions_ids LIKE '%"+permissionId+"%'");
    }

    public List<Role> getListChildren(String username) {
        List<Role> childrenRoles = new ArrayList<>();
        try {
            User currentUser = userService.findByUserName(username);
            List<Role> roles = getAll();
            for (Role role : roles) {
                if (currentUser.operationIds.containsAll(role.operationIds)) {
                    childrenRoles.add(role);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return childrenRoles;
    }
}
