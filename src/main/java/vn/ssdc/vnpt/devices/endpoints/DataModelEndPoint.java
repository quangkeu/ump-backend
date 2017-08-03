package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.*;

import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by kiendt on 1/23/2017.
 */
@Component
@Path("data-model")
@Produces(APPLICATION_JSON)
@Api("DataModels")
public class DataModelEndPoint extends SsdcCrudEndpoint<Long, DeviceTypeVersion> {
    private static final Logger logger = LoggerFactory.getLogger(DataModelEndPoint.class);

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private TagService tagService;

    @Autowired
    public DataModelEndPoint(DataModelService dataModelService) {
        this.service = this.dataModelService = dataModelService;
    }

    @GET
    @Path("/smart-create/{deviceId}")
    public void createDataModel(@PathParam("deviceId") String deviceId) {
        // create deviceType
        dataModelService.addDataModel(deviceId);
    }

    @GET
    @Path("/export/{deviceTypeVersionId}")
    public Response exportDataModelXML(@PathParam("deviceTypeVersionId") String deviceTypeVersionId) {
        String path = dataModelService.exportDataModelXML(deviceTypeVersionId);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

    @GET
    @Path("/exportJson/{deviceTypeVersionId}")
    public Response exportDataModelJson(@PathParam("deviceTypeVersionId") Long deviceTypeVersionId) {
        String path = dataModelService.exportDataModelJson(deviceTypeVersionId);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }
}