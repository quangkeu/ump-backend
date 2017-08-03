package vn.ssdc.vnpt.performance.endpoints;

import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.performance.model.*;
import vn.ssdc.vnpt.performance.sevices.PerformanceSettingService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.File;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 21-Jun-17.
 */
@Component
@Path("performance")
@Api("Performance")
@Produces(APPLICATION_JSON)
public class PerformanceSettingEndpoint extends SsdcCrudEndpoint<Long,PerformanceSetting> {

    public static final String PRESET = "PERFORMANCE SETTING ";

    private static final Logger logger = LoggerFactory.getLogger(PerformanceSettingEndpoint.class);

    @Autowired
    private PerformanceSettingService performanceSettingService;

    @Autowired
    public PerformanceSettingEndpoint(PerformanceSettingService service) {
        this.service = service;
    }

    @Autowired
    AcsClient acsClient;

    @Autowired
    DeviceGroupService deviceGroupService;

    @POST
    public PerformanceSetting doCreate(PerformanceSetting entity) {
        PerformanceSetting performanceSetting = service.create(entity);
        try {
            performanceSettingService.createQuartzJob(performanceSetting.start, performanceSetting.end, performanceSetting.id, performanceSetting.stasticsInterval);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return performanceSetting;
    }

    @PUT
    @Path("/{id}")
    public PerformanceSetting doUpdate(@PathParam("id") Long id, PerformanceSetting entity) {
        PerformanceSetting performanceSetting = service.update(id,entity);
        try {
            performanceSettingService.deleteQuartzJob(performanceSetting.id);
            performanceSettingService.deleteTriger(performanceSetting.id);
            performanceSettingService.createQuartzJob(performanceSetting.start, performanceSetting.end, performanceSetting.id, performanceSetting.stasticsInterval);

            acsClient.deletePolicyPreset(PRESET + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return performanceSetting;
    }

    @DELETE
    @Path("/{id}")
    public void doDelete(@PathParam("id") Long id) {
        try {
            performanceSettingService.deleteQuartzJob(id);
            performanceSettingService.deleteTriger(id);

            acsClient.deletePolicyPreset(PRESET + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        service.delete(id);
    }

    @GET
    @Path("/getPerformanceByPage")
    public List<PerformanceSetting> getAlarmByPage(@QueryParam("offset") int offset,
                                          @QueryParam("limit") int limit) {
        return performanceSettingService.findByPage(offset, limit);
    }


    @GET
    @Path("/search")
    public List<PerformanceSetting> getAllTask(@QueryParam("traffic") String traffic,
                                      @QueryParam("monitoring") String monitoring,
                                      @QueryParam("startDate") String startDate,
                                      @QueryParam("endDate") String endDate,
                                               @QueryParam("prefix") String prefix) {
        return performanceSettingService.search(traffic, monitoring, startDate, endDate, prefix);
    }

    @GET
    @Path("/searchPerformanceStatitics")
    public List<PerformanceStatiticsELK> searchPerformanceStatitics(@QueryParam("deviceId") String deviceId,
                                                                    @QueryParam("performanceSettingId") String performanceSettingId,
                                                                    @QueryParam("startDate") String startDate,
                                                                    @QueryParam("endDate") String endDate) {
        return performanceSettingService.searchPerformanceStatitics(deviceId, performanceSettingId, startDate, endDate);
    }

    @POST
    @Path("/deleteStatiticsInterface")
    public boolean deleteStatiticsInterface(Map<String, String> request) {
        return performanceSettingService.deleteStatiticsInterface(request.get("deviceId"),
                request.get("performanceSettingId"), request.get("stasticsInterface"));
    }

    @GET
    @Path("/get-performance-by-group")
    public List<PerformanceSetting> findPerformanceByDeviceGroup(@QueryParam("groupId") String groupId) {
        return performanceSettingService.getListPerformanceByDeviceGroupId(groupId);
    }

    @GET
    @Path("/exportExcel/{deviceGroupId}/{performanceSettingId}/{type}/{startTime}/{endTime}/{wanMode}/{manufacturer}/{modelName}/{serialNumber}/{monitoring}")
    public Response exportExcel(@PathParam("deviceGroupId") String deviceGroupId,
                                @PathParam("performanceSettingId") String performanceSettingId,
                                @PathParam("type") String type,
                                @PathParam("startTime") String startTime,
                                @PathParam("endTime") String endTime,
                                @PathParam("wanMode") String wanMode,
                                @PathParam("manufacturer") String manufacturer,
                                @PathParam("modelName") String modelName,
                                @PathParam("serialNumber") String serialNumber,
                                @PathParam("monitoring") String monitoring) {
        String path = performanceSettingService.exportExcel(deviceGroupId, performanceSettingId, type,
                startTime.replace("T"," "), endTime.replace("T"," "),
                wanMode, manufacturer, modelName, serialNumber, monitoring);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

    @GET
    @Path("/excute/{performanceSettingId}")
    public void excute(@PathParam("performanceSettingId") Long performanceSettingId, Map<String, String> request){
        performanceSettingService.statiticsData(performanceSettingId, request.get("startTime"), request.get("endTime"));
    }
}
