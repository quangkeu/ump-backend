package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Device;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 06-Feb-17.
 */
@Component
@Path("device-group")
@Api("DeviceGroups")
@Produces(APPLICATION_JSON)
public class DeviceGroupEndpoint extends SsdcCrudEndpoint<Long, DeviceGroup> {
    private DeviceGroupService deviceGroupService;

    @Autowired
    public DeviceGroupEndpoint(DeviceGroupService service) {
        this.service = this.deviceGroupService = service;
    }

    @GET
    @Path("/find-by-name")
    public List<DeviceGroup> findByName(@QueryParam("name") String name) {
        return this.deviceGroupService.findByName(name);
    }

    @GET
    @Path("/find-by-page")
    public List<DeviceGroup> getAllTask(@QueryParam("offset") int offset,
                                        @QueryParam("limit") int limit,
                                        @QueryParam("deviceGroupIds") String deviceGroupIds) {
        return deviceGroupService.findByPage(offset, limit, deviceGroupIds);
    }

    @GET
    @Path("/find-by-group-id")
    public List<Device> getListDeviceByGroup(@QueryParam("deviceGroupId") String deviceGroupId) {
        return deviceGroupService.getAllListDeviceByGroup(Long.valueOf(deviceGroupId));
    }

    @GET
    @Path("/find-all")
    public List<DeviceGroup> findAllByDeviceGroupIds(@QueryParam("deviceGroupIds") String deviceGroupIds) {
        return deviceGroupService.findAllByDeviceGroupIds(deviceGroupIds);
    }

    @GET
    @Path("/build-mongo-query")
    public String buildMongoQuery(@QueryParam("manufacturer") String manufacturer,
                                  @QueryParam("modelName") String modelName,
                                  @QueryParam("firmwareVersion") String firmwareVersion,
                                  @QueryParam("label") String label) {
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.manufacturer = manufacturer;
        deviceGroup.modelName = modelName;
        deviceGroup.firmwareVersion = firmwareVersion;
        deviceGroup.label = label;
        return deviceGroupService.buildMongoQuery(deviceGroup, false);
    }

}
