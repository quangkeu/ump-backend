package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.*;

/**
 * Created by vietnq on 11/1/16.
 */
@Service
public class DeviceTypeVersionService extends SsdcCrudService<Long, DeviceTypeVersion> {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeVersionService.class);

    private static final String SOFTWARE_VERSION_KEY = "summary.softwareVersion";
    private static final String DEVICEID_KEY = "_deviceId";

    @Autowired
    private TagService tagService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    public DeviceTypeVersionService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceTypeVersion.class);
    }

    /**
     * Finds previous version of a device
     *
     * @param deviceTypeVersionID
     * @return a DeviceType object created right before device type with id deviceTypeID
     */
    public DeviceTypeVersion prev(Long deviceTypeVersionID) {
        DeviceTypeVersion deviceTypeVersion = get(deviceTypeVersionID);
        return prev(deviceTypeVersion);
    }

    @Override
    public DeviceTypeVersion get(Long aLong) {
        DeviceTypeVersion deviceTypeVersion = super.get(aLong);
        if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
            deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
        }
        if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
            deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
        }
        return deviceTypeVersion;
    }

    public List<DeviceTypeVersion> findByDeviceType(Long deviceTypeId) {
        StringBuilder sb = new StringBuilder("device_type_id=?");
        Sort sort = new Sort(Sort.Direction.DESC, "created");
        return this.repository.search(sb.toString(), sort, deviceTypeId);
    }

    public DeviceTypeVersion findByPk(Long deviceTypeId, String version) {
        String whereExp = "device_type_id=? and firmware_version=?";
        List<DeviceTypeVersion> versions = this.repository.search(whereExp, deviceTypeId, version);
        if (!ObjectUtils.empty(versions)) {
            DeviceTypeVersion deviceTypeVersion = versions.get(0);
            if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
                deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
            }
            if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
                deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
            }
            return deviceTypeVersion;
        }
        return null;
    }

    public List<DeviceTypeVersion> findByDeviceTypeAndVersion(Long deviceTypeId, String version) {
        String whereExp = "device_type_id=? and firmware_version=?";
        List<DeviceTypeVersion> versions = this.repository.search(whereExp, deviceTypeId, version);
        if (!ObjectUtils.empty(versions)) {
            for (DeviceTypeVersion deviceTypeVersion : versions) {
                if (!ObjectUtils.empty(deviceTypeVersion.parameters)) {
                    deviceTypeVersion.parameters = new TreeMap<String, Parameter>(deviceTypeVersion.parameters);
                }
                if (!ObjectUtils.empty(deviceTypeVersion.diagnostics)) {
                    deviceTypeVersion.diagnostics = new TreeMap<String, Tag>(deviceTypeVersion.diagnostics);
                }
            }
            return versions;
        }
        return null;
    }

    public DeviceTypeVersion findbyDevice(String deviceId) {
        String paramters = "summary.softwareVersion,_deviceId._OUI,_deviceId._Manufacturer,_deviceId._ProductClass";
        ResponseEntity response = acsClient.getDevice(deviceId, paramters);
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        String version = "";
        if (array.size() > 0) {
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get(SOFTWARE_VERSION_KEY) != null && object.get(SOFTWARE_VERSION_KEY).getAsJsonObject().get("_value") != null) {
                version = object.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString();
            }
            String manufacture = object.get(DEVICEID_KEY).getAsJsonObject().get("_Manufacturer").getAsString();
            String productClass = object.get(DEVICEID_KEY).getAsJsonObject().get("_ProductClass").getAsString();
            String oui = object.get(DEVICEID_KEY).getAsJsonObject().get("_OUI").getAsString();
            DeviceType deviceType = deviceTypeService.findByPk(manufacture, oui, productClass);
            if (deviceType != null && version != "")
                return findByPk(deviceType.id, version);
        }
        return null;
    }

    private DeviceTypeVersion prev(DeviceTypeVersion deviceTypeVersion) {
        StringBuilder where = new StringBuilder("device_type_id=? and created < ?");
        Sort sort = new Sort(Sort.Direction.DESC, "created");
        List<DeviceTypeVersion> deviceTypeVersions = this.repository.search(where.toString(),
                sort, deviceTypeVersion.deviceTypeId, deviceTypeVersion.created);
        if (!ObjectUtils.empty(deviceTypeVersions)) {
            return deviceTypeVersions.get(0);
        }
        return null;
    }

    @Override
    public void afterCreate(DeviceTypeVersion entity) {
        logger.info("cloning last version parameters and tags");
        //clone parameters and  tags from older version
        DeviceTypeVersion prev = prev(entity);
        if (!ObjectUtils.empty(prev)) {
            logger.info("found last version #{}", prev.firmwareVersion);
            List<Tag> tags = tagService.findByDeviceTypeVersion(prev.id);
            for (Tag tag : tags) {
                tag.id = null;
                tag.deviceTypeVersionId = entity.id;
                tagService.create(tag);
            }
            entity.parameters = prev.parameters;
            this.repository.update(entity);
        }
        logger.info("finished cloning tags and parameters");
    }

    public DeviceTypeVersion findByFirmwareVersion(String firmwareVersion) {
        String whereExp = "firmware_version=?";
        List<DeviceTypeVersion> deviceTypeVersions = this.repository.search(whereExp, firmwareVersion);
        if (!ObjectUtils.empty(deviceTypeVersions) && !deviceTypeVersions.isEmpty()) {
            return deviceTypeVersions.get(0);
        }
        return null;
    }

    public List<DeviceTypeVersion> findByManufacturer(String manufacturer) {
        String whereExp = "manufacturer=?";
        List<DeviceTypeVersion> deviceTypeVersions = this.repository.search(whereExp, manufacturer);
        return deviceTypeVersions;
    }

    public List<DeviceTypeVersion> searchDevices(String limit, String indexPage, String deviceTypeId) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        Page<DeviceTypeVersion> all = null;
        if (!deviceTypeId.trim().equals("")) {
            deviceTypeVersions = this.repository.search("device_type_id=?", deviceTypeId);
        } else {
            all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit)));
            deviceTypeVersions = all.getContent();
        }
        return deviceTypeVersions;
    }


    public Map<String, DeviceTypeVersion> convertIdToDeviceTypeVersion(List<Tag> tagList) {
        Map<String, DeviceTypeVersion> deviceTypeVersionSet = new HashMap<String, DeviceTypeVersion>();
        for (Tag tag : tagList) {
            DeviceTypeVersion deviceTypeVersion = this.repository.findOne(tag.deviceTypeVersionId);
            DeviceType deviceType = deviceTypeService.get(deviceTypeVersion.deviceTypeId);
            deviceTypeVersionSet.put(deviceType.manufacturer + deviceType.productClass + deviceTypeVersion.firmwareVersion, deviceTypeVersion);
        }
        return deviceTypeVersionSet;

    }

    public List<DeviceTypeVersion> getDeviceTypeIDForSortAndSearch(String manufacturer, String modelName, String sort, String limit, String indexPage) {
        String whereExp = "";
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        Page<DeviceTypeVersion> all = null;
        if (sort != null && (!manufacturer.equals("All") || !modelName.equals("All"))) {
            // search + sort
            String[] sortSplit = sort.split(":");
            Sort sortField = null;
            if (!manufacturer.equals("All") && (modelName.equals("All") || modelName.equals(""))) {
                whereExp += "manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), manufacturer).getContent();

            } else if (!modelName.equals("All") && (manufacturer.equals("All") || manufacturer.equals(""))) {
                whereExp += "model_name=?";
                sortField = new Sort(Sort.Direction.ASC, sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), modelName).getContent();
            } else if (!modelName.equals("All") && !manufacturer.equals("All")
                    && !modelName.equals("") && !manufacturer.equals("")) {
                whereExp += "model_name=? and manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name", sortSplit[0]);
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name", sortSplit[0]);
                }
                deviceTypeVersions = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        sortField), modelName, manufacturer).getContent();
            }
        } else if (sort != null && !sort.equals("null")) {
            // sort
            String[] sortSplit = sort.split(":");
            if (sortSplit[1].contains("-1")) {
                all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        Sort.Direction.DESC, sortSplit[0]));
            } else {
                all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit),
                        Sort.Direction.ASC, sortSplit[0]));
            }
            deviceTypeVersions = all.getContent();
        } else {
            Sort sortRequried = new Sort(Sort.Direction.ASC, "manufacturer")
                    .and(new Sort(Sort.Direction.ASC, "created"));
            all = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), sortRequried));
            deviceTypeVersions = all.getContent();
        }

        return deviceTypeVersions;
    }

    public int countDeviceTypeIDForSortAndSearch(String manufacturer, String modelName, String sort, String limit, String indexPage) {
        String whereExp = "";
        int count = 0;
        if (sort != null && (!manufacturer.equals("All") || !modelName.equals("All"))) {
            // search + sort
            String[] sortSplit = sort.split(":");
            Sort sortField = null;
            if (!manufacturer.equals("All") && (modelName.equals("All") || modelName.equals(""))) {
                whereExp += "manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "manufacturer");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "manufacturer");
                }
                count = this.repository.search(whereExp, sortField, manufacturer).size();

            } else if (!modelName.equals("All") && (manufacturer.equals("All") || manufacturer.equals(""))) {
                whereExp += "model_name=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name");
                }
                count = this.repository.search(whereExp, sortField, modelName).size();
            } else if (!modelName.equals("All") && !manufacturer.equals("All")
                    && !modelName.equals("") && !manufacturer.equals("")) {
                whereExp += "model_name=? and manufacturer=?";
                sortField = new Sort(Sort.Direction.ASC, "model_name", "manufacturer");
                if (sortSplit[1].contains("-1")) {
                    sortField = new Sort(Sort.Direction.DESC, "model_name", "manufacturer");
                }
                count = this.repository.search(whereExp, sortField, modelName, manufacturer).size();
            }
        } else if (sort != null && !sort.equals("null")) {
            // sort
            String[] sortSplit = sort.split(":");
            if (sortSplit[1].contains("-1")) {
                count = this.repository.findAll(new Sort(Sort.Direction.DESC, sortSplit[0])).size();
            } else {
                count = this.repository.findAll(new Sort(Sort.Direction.ASC, sortSplit[0])).size();
            }

        } else {
            Sort sortRequried = new Sort(Sort.Direction.ASC, "manufacturer")
                    .and(new Sort(Sort.Direction.ASC, "created"));
            count = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), sortRequried)).getContent().size();
        }
        return count;
    }

    public Map<String, Long> generateDeviceTypeVersionWithDeviceId() {
        Map<String, Long> map = new HashMap<String, Long>();
        List<DeviceType> deviceTypeList = deviceTypeService.getAll();
        for (DeviceType tmp : deviceTypeList) {
            Long deviceTypeId = tmp.id;
            List<DeviceTypeVersion> deviceTypeVersionList = findByDeviceType(deviceTypeId);
            for (DeviceTypeVersion deviceTypeVersion : deviceTypeVersionList) {
                map.put(tmp.manufacturer + "@@@" + tmp.productClass + "@@@" + deviceTypeVersion.firmwareVersion, deviceTypeVersion.id);
            }
        }
        return map;
    }

    public Page<DeviceTypeVersion> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public List<DeviceTypeVersion> findByManufacturerAndModelName(String manufacturer, String modelName) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        deviceTypeVersions = this.repository.search("model_name=? and manufacturer=?", modelName,manufacturer);
        return deviceTypeVersions;
    }

    public List<DeviceTypeVersion> findByManufacturerAndModelNameAndFrimware(String manufacturer, String modelName, String firmware_version) {
        List<DeviceTypeVersion> deviceTypeVersions = new ArrayList<DeviceTypeVersion>();
        deviceTypeVersions = this.repository.search("model_name=? and manufacturer=? and firmware_version=?", modelName,manufacturer,firmware_version);
        return deviceTypeVersions;
    }

    public String pingDevice(String ipDevice){
        ResponseEntity response = acsClient.pingDevice(ipDevice);
        String body = (String) response.getBody();
        if(!response.toString().contains("200 OK,PING")){
            body = "Error Ping To "+ipDevice+" . Please Try Again Later !" ;
        }
        return body;
    }
}
