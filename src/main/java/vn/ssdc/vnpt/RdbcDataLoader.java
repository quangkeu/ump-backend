package vn.ssdc.vnpt;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.user.model.Operation;
import vn.ssdc.vnpt.user.model.Permission;
import vn.ssdc.vnpt.user.model.Role;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.OperationService;
import vn.ssdc.vnpt.user.services.PermissionsService;
import vn.ssdc.vnpt.user.services.RoleService;
import vn.ssdc.vnpt.user.services.UserService;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by Lamborgini on 6/2/2017.
 */
@Component
public class RdbcDataLoader implements ApplicationRunner {

    Logger logger = LoggerFactory.getLogger(RdbcDataLoader.class);
    private static final String ROOT_FILE = "operation/rdbcImport.xml";

    private static final String KEY_NAME = "@name";
    private static final String KEY_ID = "@id";

    @Autowired
    OperationService operationService;

    @Autowired
    PermissionsService permissionsService;

    @Autowired
    RoleService roleService;

    @Autowired
    UserService userService;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Override
    public void run(ApplicationArguments applicationArguments) {
        try {
            DeviceGroup allDeviceGroup = initAllDeviceGroup();
//            if (operationService.getAll().size() == 0) {
            Document document = readFile(ROOT_FILE);
            List<Node> listFileImport = document.selectNodes("//rdbc/group");
            for (Node node : listFileImport) {
                processNodeImport(node);
            }

            List<Operation> operationsList = operationService.getAll();
            String[] addOperations = new String[operationsList.size()];
            for (int i = 0; i < operationsList.size(); i++) {
                addOperations[i] = operationsList.get(i).id;
            }
            long id = initPermission(addOperations);
            if (id != 0L) {
                Role roleSuperAdmin = initRole(addOperations, id);
                initUser(roleSuperAdmin, allDeviceGroup);
            }
//            Role roleSuperAdmin = initRole(addOperations, id);

            // Create user default
            // username: ump
            // password: ump@2016
//            initUser(roleSuperAdmin);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DeviceGroup initAllDeviceGroup(){
        DeviceGroup deviceGroup = new DeviceGroup();
        if (deviceGroupService.findByName("All").size()==0){
            deviceGroup.name = "All";
            deviceGroup.query = "{}";
            deviceGroup.manufacturer = "All";
            deviceGroup.modelName = "All";
            deviceGroup.label = "";
            deviceGroupService.create(deviceGroup);
        }
        else{
            deviceGroup = deviceGroupService.findByName("All").get(0);
        }
        return deviceGroup;
    }

    private void initUser(Role role, DeviceGroup allDeviceGroup) {
        boolean check = userService.getByName("ump");
        if (!check) {
            User user = new User();
            user.userName = "ump";
            user.fullName = "UMP";
            user.email = "ump@vnpt-technology.vn";
            user.roleIds.add(String.valueOf(role.id));
            user.roleNames.add(String.valueOf(role.name));
            user.operationIds = role.operationIds;
            user.deviceGroupIds.add(String.valueOf(allDeviceGroup.id));
            user.deviceGroupNames.add(String.valueOf(allDeviceGroup.name));
            user = userService.create(user);
            user.setEncryptedPassword("ump@2016");
            userService.update(user.id, user);
        }
    }

    private Role initRole(String[] addOperations, long id) {
        Role role = new Role();
        role.name = "SuperAdmin";
        role.description = "SuperAdmin";
        Long[] addPermissionsLong = {id};
        Set<Long> setAddPermissions = new HashSet<Long>();
        Collections.addAll(setAddPermissions, addPermissionsLong);
        role.permissionsIds = setAddPermissions;
        Set<String> setAddOperations = new HashSet<String>();
        Collections.addAll(setAddOperations, addOperations);
        role.operationIds = setAddOperations;
        roleService.create(role);

        return role;
    }

    private long initPermission(String[] addOperations) {
        boolean check = permissionsService.getByName("SuperAdmin");
        if (!check) {
            Permission permissions = new Permission();
            permissions.name = "SuperAdmin";
            permissions.groupName = "SuperAdmin";
            permissions.description = "SuperAdmin";

            Set<String> setAddOperations = new HashSet<String>();
            Collections.addAll(setAddOperations, addOperations);
            permissions.operationIds = setAddOperations;
            Permission permission1 = permissionsService.create(permissions);
            return permission1.id;
        }
        return 0L;
    }

//    private void processNodeImport(Node node) {
//        String nameFile = node.valueOf(KEY_NAME);
//        List<Node> listFile = node.selectNodes("operation");
//        for (Node file : listFile) {
//            String fileId = file.valueOf(KEY_ID);
//            String fileName = file.valueOf(KEY_NAME);
//            Operation operation = new Operation();
//            operation.groupName = nameFile;
//            operation.id = fileId;
//            operation.name = fileName;
//            operationService.create(operation);
//        }
//    }

    private void processNodeImport(Node node) {
        String nameFile = node.valueOf(KEY_NAME);
        List<Node> listFile = node.selectNodes("operation");
        for (Node file : listFile) {
            String fileId = file.valueOf(KEY_ID);
            String fileName = file.valueOf(KEY_NAME);
            boolean result = operationService.getById(fileId);
            if (!result) {
                Operation operation = new Operation();
                operation.groupName = nameFile;
                operation.id = fileId;
                operation.name = fileName;
                operationService.create(operation);
            }
        }
    }

    private Document readFile(String fileName) throws DocumentException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        SAXReader reader = new SAXReader();
        Document document = reader.read(is);
        return document;

    }
}
