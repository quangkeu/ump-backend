package vn.ssdc.vnpt.devices.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vietnq on 11/1/16.
 */
@Service
public class DeviceTypeService extends SsdcCrudService<Long, DeviceType> {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTypeService.class);

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceType.class);
    }

    public DeviceType findByPk(String manufacturer, String oui, String productClass) {
        String whereExp = "manufacturer=? and oui=? and product_class=?";
        List<DeviceType> deviceTypes = this.repository.search(whereExp, manufacturer, oui, productClass);
        if (!ObjectUtils.empty(deviceTypes)) {
            return deviceTypes.get(0);
        }
        return null;
    }

    public DeviceType findByPk(String oui, String productClass) {
        String whereExp = "oui=? and product_class=?";
        List<DeviceType> deviceTypes = this.repository.search(whereExp, oui, productClass);
        if (!ObjectUtils.empty(deviceTypes)) {
            return deviceTypes.get(0);
        }
        return null;
    }


    @Override
    public void beforeDelete(Long id) {
        List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByDeviceType(id);
        if (!ObjectUtils.empty(deviceTypeVersions) && !deviceTypeVersions.isEmpty()) {
            throw new NotFoundException("Object is used.");
        }
    }

    public boolean isExisted(Long id, String name, String manufacturer, String oui, String productClass) {
        String whereExp = "id<>? AND name=? AND manufacturer=? AND oui=? AND product_class=?";
        List<DeviceType> deviceTypes = this.repository.search(whereExp, id, name, manufacturer, oui, productClass);
        return !ObjectUtils.empty(deviceTypes) && deviceTypes.isEmpty();
    }

    public List<DeviceType> findByManufacturerAndModelName(String manufacturer, String modelName) {
        List<DeviceType> deviceTypes = new ArrayList<DeviceType>();
        deviceTypes = this.repository.search("model_name=? and manufacturer=?", modelName,manufacturer);
        return deviceTypes;
    }
}
