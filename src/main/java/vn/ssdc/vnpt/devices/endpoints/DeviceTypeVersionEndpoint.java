package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by vietnq on 11/1/16.
 */
@Component
@Path("device-type-versions")
@Produces(APPLICATION_JSON)
@Api("DeviceTypeVersions")
public class DeviceTypeVersionEndpoint extends SsdcCrudEndpoint<Long, DeviceTypeVersion> {
    private TagService tagService;
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeVersionEndpoint(DeviceTypeVersionService deviceTypeVersionService,
                                     TagService tagService) {
        this.service = this.deviceTypeVersionService = deviceTypeVersionService;
        this.tagService = tagService;
    }

    @GET
    @Path("/{id}/tags")
    public List<Tag> findTags(@PathParam("id") Long id) {
        return tagService.findByDeviceTypeVersion(id);
    }

    @GET
    @Path("/{id}/assigned-tags")
    public List<Tag> findAssignedTags(@PathParam("id") Long id) {
        return tagService.findAssignedTags(id);
    }

    @POST
    @Path("/{id}/tags")
    public List<Tag> assignTags(@PathParam("id") Long id, List<Long> tagsID) {
        List<Tag> tags = new ArrayList<Tag>();
        for (Long tagID : tagsID) {
            Tag tag = tagService.get(tagID);
            tag.assigned = 1;
            tagService.update(tagID, tag);
            tags.add(tag);
        }
        return tags;
    }

    @GET
    @Path("/{id}/prev")
    public DeviceTypeVersion prev(@PathParam("id") Long id) {
        return this.deviceTypeVersionService.prev(id);
    }

    @GET
    @Path("/find-by-pk")
    public DeviceTypeVersion findByPk(@QueryParam("deviceTypeId") Long deviceTypeId,
                                      @QueryParam("version") String firmwareVersion) {

        return this.deviceTypeVersionService.findByPk(deviceTypeId, firmwareVersion);

    }

    @GET
    @Path("/find-by-firmware-version")
    public DeviceTypeVersion findByFirmwareVersion(@QueryParam("firmwareVersion") String firmwareVersion) {
        return this.deviceTypeVersionService.findByFirmwareVersion(firmwareVersion);
    }

    @GET
    @Path("/find-by-manufacturer")
    public List<DeviceTypeVersion> findByManufacturer(@QueryParam("manufacturer") String manufacturer) {
        return this.deviceTypeVersionService.findByManufacturer(manufacturer);
    }

    @GET
    @Path("/find-by-device")
    public DeviceTypeVersion findByDevice(@QueryParam("deviceId") String deviceId) {
        return this.deviceTypeVersionService.findbyDevice(deviceId);
    }

    @GET
    @Path("/search-devices")
    public List<DeviceTypeVersion> searchDevices(
            @DefaultValue("20") @QueryParam("limit") String limit,
            @DefaultValue("0") @QueryParam("indexPage") String indexPage,
            @DefaultValue("") @QueryParam("deviceTypeId") String deviceTypeId) {
        return this.deviceTypeVersionService.searchDevices(limit, indexPage, deviceTypeId);
    }

    @POST
    @Path("/get-device-type-id-for-sort-and-search")
    public List<DeviceTypeVersion> getDeviceTypeIDForSortAndSearch(
            Map<String, String> requestParam,
            @DefaultValue("created:-1") @QueryParam("sort") String sort,
            @DefaultValue("20") @QueryParam("limit") String limit,
            @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.getDeviceTypeIDForSortAndSearch(manufacturer, modelName, sort, limit, indexPage);
    }

    @POST
    @Path("/count-device-type-id-for-sort-and-search")
    public int countDeviceTypeIDForSortAndSearch(
            Map<String, String> requestParam,
            @DefaultValue("created:-1") @QueryParam("sort") String sort,
            @DefaultValue("20") @QueryParam("limit") String limit,
            @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.countDeviceTypeIDForSortAndSearch(manufacturer, modelName, sort, limit, indexPage);
    }

    //unused
    @GET
    @Path("/find-all-device-type-id")
    public Map<String, Long> getWithDeviceTypeId() {
        return this.deviceTypeVersionService.generateDeviceTypeVersionWithDeviceId();
    }

    @GET
    @Path("/get-page")
    public Page<DeviceTypeVersion> getPage(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return this.deviceTypeVersionService.getPage(page, limit);
    }

    @POST
    @Path("/find-by-manufacturer-and-modelName")
    public List<DeviceTypeVersion> findByManufacturerAndModelName(Map<String, String> requestParam) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.findByManufacturerAndModelName(manufacturer, modelName);
    }

    @POST
    @Path("/find-by-manu-and-model-and-firm")
    public List<DeviceTypeVersion> findByManufacturerAndModelNameAndFirmware(Map<String, String> requestParam) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        String firmware = requestParam.get("firmware");
        return this.deviceTypeVersionService.findByManufacturerAndModelNameAndFrimware(manufacturer, modelName, firmware);
    }

    @POST
    @Path("/pingDevice")
    public String pingDevice(Map<String, String> requestParam) {
        return this.deviceTypeVersionService.pingDevice(requestParam.get("ipDevice"));
    }
}
