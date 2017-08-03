package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by vietnq on 11/1/16.
 */
@Component
@Path("device-types")
@Produces(APPLICATION_JSON)
@Api("DeviceTypes")
public class DeviceTypeEndpoint extends SsdcCrudEndpoint<Long, DeviceType> {

    private TagService tagService;
    private DeviceTypeService deviceTypeService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeEndpoint(DeviceTypeService deviceTypeService,
                              TagService tagService) {
        this.service = this.deviceTypeService = deviceTypeService;
        this.tagService = tagService;
    }

    @GET
    @Path("/{deviceTypeID}/versions")
    public List<DeviceTypeVersion> findVersions(@PathParam("deviceTypeID") Long deviceTypeID) {
        return deviceTypeVersionService.findByDeviceType(deviceTypeID);
    }

    @GET
    @Path("/find-by-pk")
    public DeviceType findByPk(@QueryParam("manufacturer") String manufacturer,
                               @QueryParam("oui") String oui,
                               @QueryParam("productClass") String productClass) {
        return deviceTypeService.findByPk(manufacturer, oui, productClass);
    }

    // unused
    @GET
    @Path("/is-existed")
    public boolean isExisted(@QueryParam("id") Long id,
                             @QueryParam("manufacturer") String manufacturer,
                             @QueryParam("name") String name,
                             @QueryParam("oui") String oui,
                             @QueryParam("productClass") String productClass) {
        return deviceTypeService.isExisted(id, name, manufacturer, oui, productClass);
    }

    @POST
    @Path("/find-by-manufacturer-and-modelName")
    public List<DeviceType> findByManufacturerAndModelName(Map<String, String> requestParam) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeService.findByManufacturerAndModelName(manufacturer, modelName);
    }
}
