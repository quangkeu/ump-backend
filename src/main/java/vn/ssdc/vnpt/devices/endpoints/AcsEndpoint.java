package vn.ssdc.vnpt.devices.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.*;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.vnpt.ssdc.event.AMQPSubscribes;
import vn.vnpt.ssdc.event.Event;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * REST API for Device Management <br/>
 * <p>
 * Created by vietnq on 10/31/16.
 */
@Component
@Path("acs")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Devices")
public class AcsEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(AcsEndpoint.class);
    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private TagService tagService;

    @Autowired
    private DiagnosticService diagnosticService;

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private BlackListDeviceService blackListDeviceService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    /**
     * Search for Devices
     *
     * @param query      query in mongo syntax
     * @param limit
     * @param offset
     * @param parameters List of selected parameter, separated by comma
     * @param sort
     * @return
     */
    @GET
    @ApiOperation(value = "Get devices  from central data storage")
    @ApiResponse(code = 200, message = "Success", response = AcsResponse.class)
    public AcsResponse searchDevices(@ApiParam(required = false, value = "Query string in mongo db syntax") @DefaultValue("{}") @QueryParam("query") String query,
                                     @ApiParam(value = "Number of returned devices, default is 50") @DefaultValue("20") @QueryParam("limit") String limit,
                                     @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("offset") String offset,
                                     @ApiParam(value = "List of selected parameters, separated by comma", example = "_deviceId,summary") @QueryParam("parameters") String parameters,
                                     @ApiParam(value = "Sorting option, in mongodb syntax", example = "{\"_registered\":-1}") @DefaultValue("{\"_registered\":-1}") @QueryParam("sort") String sort) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", query);
        queryParams.put("limit", limit);
        queryParams.put("skip", offset);
        if (!ObjectUtils.empty(parameters)) {
            queryParams.put("projection", parameters);
        }
        if (!ObjectUtils.empty(sort)) {
            queryParams.put("sort", sort);
        }


        ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("total").get(0));
        response.body = responseEntity.getBody();
        response.totalCount = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));

        return response;
    }

    /**
     * Reboot a specific device.
     *
     * @param deviceId The ID of the device
     * @return 202 if the tasks have been queued to be executed at the next inform.
     * 500 Internal server error
     * status code 200 if tasks have been successfully executed
     */
    @POST
    @Path(("/{deviceId}/reboot"))
    public AcsResponse reboot(@PathParam("deviceId") String deviceId,
                              @DefaultValue("false") @QueryParam("now") String now) {
        AcsResponse response = new AcsResponse();
        try {
            ResponseEntity<String> responseEntity = acsClient.reboot(deviceId, Boolean.valueOf(now));
            response.httpResponseCode = responseEntity.getStatusCode().value();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    /**
     * Factory reset a specific device.
     *
     * @param deviceId
     * @return 202 if the tasks have been queued to be executed at the next inform.
     * 500 Internal server error
     * status code 200 if tasks have been successfully executed
     */
    @POST
    @Path(("/{deviceId}/factory-reset"))
    public AcsResponse factoryReset(@PathParam("deviceId") String deviceId,
                                    @DefaultValue("false") @QueryParam("now") String now) {
        AcsResponse response = new AcsResponse();
        try {
            ResponseEntity<String> responseEntity = acsClient.factoryReset(deviceId, Boolean.valueOf(now));
            response.httpResponseCode = responseEntity.getStatusCode().value();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.NOT_FOUND.value();
        }
        return response;
    }

    /**
     * Gets parameter values for a device
     * Ask ACS to execute a task to get values for given parameters
     *
     * @param deviceId id of device in ACS
     * @param request  a map containing keys "now" and "parameters"
     * @return AcsResponse object
     */
    @POST
    @Path("/{deviceId}/get-parameter-values")
    public AcsResponse getParameterValues(@PathParam("deviceId") String deviceId,
                                          Map<String, Object> request) {
        List<String> parameters = (List<String>) request.get("parameters");
        Boolean now = (Boolean) request.get("now");
        ResponseEntity<String> responseEntity = this.acsClient.getParameterValues(deviceId, parameters, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        return response;
    }

    @POST
    @Path("/{deviceId}/set-parameter-values")
    public AcsResponse setParameterValues(@PathParam("deviceId") String deviceId,
                                          Map<String, Object> request) {
        Boolean now = true;
        if (request.get("now") != null) {
            now = (Boolean) request.get("now");
        }
        Map<String, Object> paramValues = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : request.entrySet()) {
            if (!"now".equals(entry.getKey())) {
                paramValues.put(entry.getKey(), entry.getValue());
            }
        }
        ResponseEntity<String> entity = this.acsClient.setParameterValues(deviceId, paramValues, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

    //refresh-object endpoint
    @POST
    @Path("/{deviceId}/refresh-object")
    public AcsResponse refreshObject(@PathParam("deviceId") String deviceId,
                                     Map<String, Object> request) {
        Boolean now = (Boolean) request.get("now");
        String objectName = (String) request.get("objectName");

        ResponseEntity<String> entity = this.acsClient.refreshObject(deviceId, objectName, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

    @POST
    @Path("/{deviceId}/downloadFile")
    public AcsResponse downloadFile(@PathParam("deviceId") String deviceId,
                                    Map<String, Object> request) {
        Boolean now = (Boolean) request.get("now");
        String fileId = (String) request.get("fileId");
        String fileName = (String) request.get("fileName");
        ResponseEntity<String> entity = this.acsClient.downloadFile(deviceId, fileId, fileName, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

    @POST
    @Path("/{deviceId}/uploadFile")
    public AcsResponse uploadFile(@PathParam("deviceId") String deviceId,
                                  Map<String, Object> request) {
        Boolean now = (Boolean) request.get("now");
        String fileType = (String) request.get("fileType");
        ResponseEntity<String> entity = this.acsClient.uploadFile(deviceId, fileType, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

    //add-object endpoint

    @POST
    @Path("/{deviceId}/add-object")
    public AcsResponse addObject(@PathParam("deviceId") String deviceId,
                                 @QueryParam("now") Boolean now,
                                 Map<String, Object> request) {
        String objectName = (String) request.get("objectName");
        Map<String, String> parameterValues = (Map<String, String>) request.get("parameterValues");
        ResponseEntity<String> responseEntity = this.acsClient.addObject(deviceId, objectName, parameterValues, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        return response;
    }

    @POST
    @Path("/{deviceId}/delete-object")
    public AcsResponse delete(@PathParam("deviceId") String deviceId,
                              @QueryParam("now") Boolean now,
                              Map<String, Object> request) {
        String objectName = (String) request.get("objectName");
        ResponseEntity<String> responseEntity = this.acsClient.deleteObject(deviceId, objectName, now);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        return response;
    }

    @GET
    @Path("/{deviceId}/{object}/checkObject")
    public Boolean checkObject(@PathParam("deviceId") String deviceId, @PathParam("object") String object) {
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.findbyDevice(deviceId);
        if (deviceTypeVersion != null) {
            ParameterDetail parameterDetail = parameterDetailService.getByTr069Name(tr069ParameterService.convertToTr069Param(object), deviceTypeVersion.id);
            if(parameterDetail != null && ("true").equals(parameterDetail.access)) return true;
        }
        return false;
    }

    @GET
    @Path("/{deviceId}/checkExisted")
    public Boolean checkExisted(@PathParam("deviceId") String deviceId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", String.format("{\"_id\":\"%s\"}", deviceId));
        ResponseEntity<String> responseEntity = this.acsClient.checkExist(deviceId, params);
        int total = Integer.parseInt(responseEntity.getHeaders().get("total").get(0));
        if (total == 0)
            return false;
        return true;
    }

    @DELETE
    @Path("/{deviceId}/delete-device")
    public boolean deleteDevice(@PathParam("deviceId") String deviceId) {
        return this.acsClient.deleteDevice(deviceId);
    }

    @POST
    @Path(("/{deviceId}/create-label/{label}"))
    public int createLabel(@PathParam("deviceId") String deviceId,
                           @PathParam("label") String label) {
        ResponseEntity<String> stringResponseEntity = this.acsClient.addLabel(deviceId, label);
        try {
            return stringResponseEntity.getStatusCodeValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @POST
    @Path("/{deviceId}/remove-label/{label}")
    public boolean removeLabel(@PathParam("deviceId") String deviceId, @PathParam("label") String label) {
        return this.acsClient.deleteLabel(deviceId, label);
    }

    @POST
    @Path("/recheck-status/{deviceId}")
    public boolean recheckStatus(@PathParam("deviceId") String deviceId) {

        try {
            ResponseEntity<String> responseEntity = this.acsClient.recheckStatus(deviceId);
            if (responseEntity.getStatusCode() == HttpStatus.OK)
                return true;
            else
                return false;
        }
        catch (final HttpClientErrorException e) {
            return false;
        }
    }

    @GET
    @Path("/get-all-files")
    public AcsResponse searchFile(@QueryParam("parameters") String parameters, @QueryParam("query") String query) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("sort", "{\"uploadDate\":-1}");
        if (!ObjectUtils.empty(parameters)) {
            queryParams.put("projection", parameters);
        }
        if (!ObjectUtils.empty(query)) {
            queryParams.put("query", query);
        }
        ResponseEntity<String> responseEntity = this.acsClient.search("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();

        return response;
    }

    @POST
    @Path("/deleteFile/{paramId}/{paramName}")
    public String deleteFile(@PathParam("paramId") String paramId,
                             @PathParam("paramName") String paramName) {
        return this.acsClient.deleteDevice("files", paramId, paramName);
    }

    @POST
    @Path("/{deviceId}/newDataModel")
    public void newDataModel(@PathParam("deviceId") String deviceId) {
        dataModelService.addDataModel(deviceId);
    }

    @POST
    @Path("/{deviceId}/newDeviceRegistered")
    public void newDeviceRegistered(@PathParam("deviceId") String deviceId) {
        processNewDeviceRegistered(deviceId);
    }

    public void processNewDeviceRegistered(String deviceId) {
        if (blackListDeviceService.findByDeviceId(deviceId).size() == 0) {
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
            JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
            if (arrayTmpObject.size() > 0) {
                JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
                JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
                String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
                String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
                String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
                DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
                if (currenDeviceType == null) {
                    this.acsClient.refreshAll(deviceId, false);
                } else {
                    synchronizeDevice(deviceId);
                }
            }
        } else {
            logger.info("New device registered #{} in black list", deviceId);
            acsClient.deleteDevice(deviceId);
        }
    }

    public void synchronizeDevice(String deviceId) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
            String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType == null) {
                this.acsClient.refreshAll(deviceId, false);
            } else {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion == null) {
                    this.acsClient.refreshAll(deviceId, false);
                } else {
                    List<Tag> lTag = tagService.findByDeviceTypeVersionIdAssignedSynchronized(currentDeviceTypeVersion.id);
                    List<String> listObjects = new ArrayList<String>();
                    List<String> listParameters = new ArrayList<String>();
                    List<String> listParameterPaths = new ArrayList<String>();
                    if (lTag != null && lTag.size() != 0) {
                        for (int index = 0; index < lTag.size(); index++) {
                            Map<String, Parameter> parameters = lTag.get(index).parameters;
                            for (Map.Entry<String, Parameter> tmp : parameters.entrySet()) {
                                Parameter parameter = new Gson().fromJson(new Gson().toJson(tmp.getValue()), Parameter.class);
                                if (!"object".equals(parameter.dataType)) {
                                    if (parameter.tr069Name.contains("{i}")) {
                                        String parentPath = parameter.tr069ParentObject.substring(0, parameter.tr069ParentObject.indexOf("{i}"));
                                        ;
                                        if (!listObjects.contains(parentPath)) {
                                            Boolean isDescent = false;
                                            for (int i = 0; i < listObjects.size(); i++) {
                                                if (parentPath.contains(listObjects.get(i))) {
                                                    isDescent = true;
                                                }
                                                if (listObjects.get(i).contains(parentPath)) {
                                                    listObjects.remove(i);
                                                }
                                            }
                                            if (!isDescent) {
                                                listObjects.add(parentPath);
                                            }
                                        }
                                    } else {
                                        listParameters.add(parameter.path);
                                    }
                                }
                            }
                            for (String path : listObjects) {
                                int indexOf = path.indexOf(".");
                                while (indexOf >= 0) {
                                    String parameterPath = path.substring(0, (indexOf + 1));
                                    if (!listParameterPaths.contains(parameterPath)) {
                                        listParameterPaths.add(parameterPath);
                                    }
                                    indexOf = path.indexOf(".", indexOf + 1);
                                }
                            }
                            for (String path : listParameters) {
                                int indexOf = path.indexOf(".");
                                while (indexOf >= 0) {
                                    String parameterPath = path.substring(0, (indexOf + 1));
                                    if (!listParameterPaths.contains(parameterPath)) {
                                        listParameterPaths.add(parameterPath);
                                    }
                                    indexOf = path.indexOf(".", indexOf + 1);
                                }
                            }
                            for (String path : listParameterPaths) {
                                acsClient.getParameterNames(deviceId, path, true,false);
                            }
                            for (String path : listObjects) {
                                acsClient.refreshObject(deviceId, path, false);
                            }
                            if (listParameters.size() > 0) {
                                acsClient.getParameterValues(deviceId, listParameters, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @GET
    @Path("/getDevices")
    public AcsResponse getDevices(@DefaultValue("") @QueryParam("serial") String serial) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", String.format("{\"_id\":\"/%s/\"}", serial));
        queryParams.put("projection", "_id");

        ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
        response.body = responseEntity.getBody();
        return response;
    }
}
