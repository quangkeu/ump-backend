package vn.ssdc.vnpt.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.user.model.Permission;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Lamborgini on 5/4/2017.
 */
@Service
public class PermissionsService extends SsdcCrudService<Long, Permission> {

    @Autowired
    public PermissionsService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Permission.class);
    }

    public List<Permission> searchPermission(String limit, String indexPage) {
        List<Permission> permissionArrayList = new ArrayList<Permission>();
        Page<Permission> all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit)));
        permissionArrayList = all.getContent();
        return permissionArrayList;
    }

    public int checkGroupName(String addGroupName, String addName) {
        String whereExp = "group_name=? and name=?";
        return this.repository.search(whereExp, addGroupName, addName).size();
    }

    public boolean getByName(String name) {
        int count = this.repository.search("name=?", name).size();
        if (count == 0) {
            return false;
        }
        return true;
    }
}
