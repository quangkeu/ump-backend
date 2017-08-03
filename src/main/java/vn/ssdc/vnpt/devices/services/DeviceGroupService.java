package vn.ssdc.vnpt.devices.services;

import com.mongodb.QueryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.Device;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.*;

/**
 * Created by thangnc on 06-Feb-17.
 */
@Service
public class DeviceGroupService extends SsdcCrudService<Long, DeviceGroup> {
    public static final String ID = "_id";
    public static final String IP = "_ip";
    public static final String SERIAL_NUMBER = "_deviceId._SerialNumber";
    public static final String MANUFACTURER = "_deviceId._Manufacturer";
    public static final String OUI = "_deviceId._OUI";
    public static final String PRODUCT_CLASS = "_deviceId._ProductClass";
    public static final String FRIMWARE_VERSION = "summary.softwareVersion";
    public static final String LAST_INFORM = "_lastInform";
    public static final String MODEL_NAME = "summary.modelName";
    public static final String LABEL = "_tags";


    private static final Logger logger = LoggerFactory.getLogger(DeviceGroupService.class);

    @Autowired
    public DeviceGroupService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceGroup.class);
    }

    @Autowired
    private AcsClient acsClient;

    public void setAcsClient(AcsClient acsClient) {
        this.acsClient = acsClient;
    }

    protected Map<String, String> deviceIndexParams() {
        return new LinkedHashMap<String, String>() {{
            // Infor. map voi key trong file message
            put(ID, "ID");
            put(SERIAL_NUMBER, "Infor.SerialNumber");
            put(MANUFACTURER, "Infor.Manufacturer");
            put(OUI, "Infor.OUI");
            put(PRODUCT_CLASS, "Infor.ProductClass");
            put(IP, "Infor.IPAddress");
            put(FRIMWARE_VERSION, "Infor.FirmwareVersion");
            put(LAST_INFORM, "Last Inform");

        }};
    }


    protected Map<String, String> deviceIndexParamsRamCpu() {
        return new LinkedHashMap<String, String>() {{
            // Infor. map voi key trong file message
            put(ID, "ID");
            put(SERIAL_NUMBER, "Infor.SerialNumber");
            put(MANUFACTURER, "Infor.Manufacturer");
            put(MODEL_NAME, "Model Name");

        }};
    }

    public List<String> getListDeviceByGroup(Long lDevice_Group_Id) {
        List<String> lReturn = new ArrayList<String>();

        Map<String, String> deviceIndexParams = deviceIndexParams();
        DeviceGroup deviceGroup = repository.findOne(lDevice_Group_Id);

        String query = deviceGroup.query;
        String parameters = StringUtils.join(",", deviceIndexParams().keySet());

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", query);
        queryParams.put("projection", parameters);

        AcsResponse response = new AcsResponse();
        ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
        response.body = responseEntity.getBody();

        List<Device> lDevice = Device.fromJsonString(response.body, deviceIndexParams.keySet());
        if (lDevice != null) {
            for (int index = 0; index < lDevice.size(); index++){
                String strId = lDevice.get(index).id;
                lReturn.add(strId);
            }
        }
        return lReturn;
    }

    public List<Device> getAllListDeviceByGroup(Long lDevice_Group_Id) {
        List<String> lReturn = new ArrayList<String>();

        Map<String, String> deviceIndexParams = deviceIndexParamsRamCpu();
        DeviceGroup deviceGroup = repository.findOne(lDevice_Group_Id);

        String query = deviceGroup.query;
        String parameters = StringUtils.join(",", deviceIndexParamsRamCpu().keySet());

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", query);
        queryParams.put("projection", parameters.substring(1));

        AcsResponse response = new AcsResponse();
        ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
        response.body = responseEntity.getBody();

        return Device.fromJsonString(response.body, deviceIndexParams.keySet());
    }

    public List<DeviceGroup> findByName(String name) {
        String whereExp = "name=?";
        List<DeviceGroup> deviceGroups = this.repository.search(whereExp, name);
        return deviceGroups;
    }

    public List<DeviceGroup> findByPage(int offset, int limit, String deviceGroupIds) {
        String whereExp = "id IN (?) order by id desc limit ?,?";
        List<DeviceGroup> deviceGroups = this.repository.search(whereExp, deviceGroupIds, (offset - 1) * limit, limit);

        // If has device group all id => get all device group
        for (DeviceGroup deviceGroup : deviceGroups) {
            if ("All".equals(deviceGroup.name)) {
                deviceGroups = getAll();
                break;
            }
        }

        return deviceGroups;
    }

    @Override
    public void beforeCreate(DeviceGroup deviceGroup) {
        deviceGroup.query = buildMongoQuery(deviceGroup, true);
        super.beforeCreate(deviceGroup);
    }

    @Override
    public void beforeUpdate(Long id, DeviceGroup deviceGroup) {
        deviceGroup.query = buildMongoQuery(deviceGroup, true);
        super.beforeUpdate(id, deviceGroup);
    }

    public String buildMongoQuery(DeviceGroup deviceGroup, boolean flag){
        QueryBuilder query = new QueryBuilder();
        if(flag){
            if(deviceGroup.manufacturer != null && !("All").equals(deviceGroup.manufacturer) && deviceGroup.modelName != null && ("All").equals(deviceGroup.modelName)) {
                query.put(MANUFACTURER).is(deviceGroup.manufacturer);
            }
            if(deviceGroup.productClass != null){
                query.put(PRODUCT_CLASS).is(deviceGroup.productClass);
            }
            if(deviceGroup.oui != null){
                query.put(OUI).is(deviceGroup.oui);
            }
            if(deviceGroup.firmwareVersion != null){
                query.put(FRIMWARE_VERSION).is(deviceGroup.firmwareVersion);
            }
        } else {
            if(!deviceGroup.manufacturer.isEmpty()){
                query.put(MANUFACTURER).is(deviceGroup.manufacturer);
            }
            if(!deviceGroup.modelName.isEmpty()){
                query.put(MODEL_NAME).is(deviceGroup.modelName);
            }
            if(!deviceGroup.firmwareVersion.isEmpty()){
                query.put(FRIMWARE_VERSION).is(deviceGroup.firmwareVersion);
            }
        }

        if(deviceGroup.label != null && !deviceGroup.label.isEmpty()) {
            //Parse label
            char charAtLast = 'a';
            int j = 0;
            boolean first = false;

            String label = deviceGroup.label.replaceAll("\"", "");
            label = label.replaceAll(" AND ", "&");
            label = label.replaceAll(" OR ", "#");
            if (label.contains("&") || label.contains("#")) {
                for (int i = 0; i < label.length(); i++) {
                    if (label.charAt(i) == '&' || label.charAt(i) == '#') {
                        if (j == 0) {
                            query.and(QueryBuilder.start(LABEL).is(label.substring(j, i)).get());
                            first = true;
                        } else {
                            if (first && charAtLast == '&') {
                                query.and(QueryBuilder.start(LABEL).is(label.substring(j, i)).get());
                            } else {
                                if (charAtLast == '&')
                                    query.and(QueryBuilder.start(LABEL).is(label.substring(j, i)).get());
                                if (charAtLast == '#')
                                    query.or(QueryBuilder.start(LABEL).is(label.substring(j, i)).get());
                            }
                            first = false;
                        }
                        j = i + 1;
                        charAtLast = label.charAt(i);
                    }
                }
            if (j > 0) {
                if (charAtLast == '&')
                    query.and(QueryBuilder.start(LABEL).is(label.substring(j, label.length())).get());
                if (charAtLast == '#')
                    query.or(QueryBuilder.start(LABEL).is(label.substring(j, label.length())).get());
            }

        } else query.put(LABEL).is(label);
        }
        return query.get().toString();
    }

    @Override
    public List<DeviceGroup> getAll() {
        String whereExp = "1=1";
        Sort sort = new Sort(Sort.Direction.ASC,"name");
        List<DeviceGroup> deviceGroups = this.repository.search(whereExp, sort);
        return deviceGroups;
    }

    public List<DeviceGroup> findAllByDeviceGroupIds(String deviceGroupIds) {
        String whereExp = "id IN (?)";
        Sort sort = new Sort(Sort.Direction.ASC,"name");
        List<DeviceGroup> deviceGroups = this.repository.search(whereExp, sort, deviceGroupIds);

        // If has device group all id => get all device group
        for (DeviceGroup deviceGroup : deviceGroups) {
            if ("All".equals(deviceGroup.name)) {
                deviceGroups = getAll();
                break;
            }
        }

        return deviceGroups;
    }
}
