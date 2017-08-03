package vn.ssdc.vnpt.devices.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClientException;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.DiagnosticService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;
import vn.vnpt.ssdc.event.AMQPSubscribes;
import vn.vnpt.ssdc.event.Event;

import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by SSDC on 11/14/2016.
 */

@Component
@Path("diagnostic")
@Api("Diagnostic")
@Produces(APPLICATION_JSON)
public class DiagnosticEndPoint extends SsdcCrudEndpoint<Long, DiagnosticTask> {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticEndPoint.class);

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DiagnosticService diagnosticService;

    @Autowired
    private TagService tagService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    public DiagnosticEndPoint(DiagnosticService diagnosticService) {
        this.service = this.diagnosticService = diagnosticService;
    }

    @POST
    @Path("/{deviceId}/{diagnosticName}/createNew")
    public long insertDiagnosticsModel(@PathParam("deviceId") String deviceID,
                                                         @PathParam("diagnosticName") String diagnosticName,
                                                         Map<String, Object> requestParam) {
        long id = 0;
        ResponseEntity<String> result = acsClient.createDiagnostic(deviceID, requestParam, true);
        if (result.getStatusCodeValue() == 200 || result.getStatusCodeValue() == 202) {
            JsonObject object = new Gson().fromJson(result.getBody(), JsonObject.class);
            String idTask = object.get("_id").getAsString();
            //insert to diagnostics task
            DiagnosticTask diagnosticTask = new DiagnosticTask();
            diagnosticTask.deviceId = deviceID;
            diagnosticTask.diagnosticsName = diagnosticName;

            ResponseEntity<String> deviceInfo = acsClient.getDevice(deviceID, "_deviceId._OUI,_deviceId._ProductClass,InternetGatewayDevice.DeviceInfo.SoftwareVersion._value");
            JsonArray deviceObject = new Gson().fromJson(deviceInfo.getBody(), JsonArray.class);
            JsonObject element = (JsonObject) deviceObject.get(0);
            String oui = element.get("summary.oui").getAsString();
            String productClass = element.get("summary.productClass").getAsString();
            JsonObject fwVersionObject = element.get("summary.softwareVersion").getAsJsonObject();
            String fwVersion = fwVersionObject.get("_value").getAsString();
//            Map<String, Tag> listDiagnostics = null;
            DeviceType deviceType = deviceTypeService.findByPk(oui, productClass);
            if (deviceType != null) {
                Long deviceTypeid = deviceType.id;
                List<DeviceTypeVersion> deviceTypeVersions = deviceTypeVersionService.findByDeviceTypeAndVersion(deviceTypeid, fwVersion);
                // If device type version = null => run refeshObject
                for (DeviceTypeVersion deviceTypeVersion : deviceTypeVersions) {
                    if(deviceTypeVersion.diagnostics.get(diagnosticName) != null) {
                        diagnosticTask.parameterFull = deviceTypeVersion.diagnostics.get(diagnosticName).parameters;
                    }
                }
//                if (deviceTypeVersion != null) {
//                    listDiagnostics = deviceTypeVersion.diagnostics;
//                }
            }
//            diagnosticTask.parameterFull = listDiagnostics.get(diagnosticName).parameters;

            diagnosticTask.request = requestParam;
            diagnosticTask.taskId = idTask;
            //
            id = diagnosticService.create(diagnosticTask).id;
        }
        return id;
    }

    @GET
    @Path("/{manufacturer}/{oui}/{productClass}/{fwVersion}/listDiagnostics")
    public List<Map<String, Tag>> getListDiagnostics(@PathParam("manufacturer") String manufacturer,
                                               @PathParam("oui") String oui,
                                               @PathParam("productClass") String productClass,
                                               @PathParam("fwVersion") String fwVersion
    ) {
        List<Map<String, Tag>> mapReturns = new LinkedList<>();
        Map<String, Tag> mapReturn = null;
        DeviceType deviceType = deviceTypeService.findByPk(manufacturer, oui, productClass);
        if (deviceType != null) {
            Long deviceTypeid = deviceType.id;
            List<DeviceTypeVersion> deviceTypeVersions= deviceTypeVersionService.findByDeviceTypeAndVersion(deviceTypeid, fwVersion);
            // If device type version = null => run refeshObject
            for (DeviceTypeVersion deviceTypeVersion : deviceTypeVersions) {
                mapReturn = deviceTypeVersion.diagnostics;
                mapReturns.add(mapReturn);
            }
        }
        return mapReturns;
    }

    @GET
    @Path("/{deviceId}/getAllTask")
    public List<DiagnosticTask> getAllTask(@PathParam("deviceId") String deviceId,
                                           @QueryParam("offset") int offset,
                                           @QueryParam("limit") int limit,
                                           @QueryParam("mode") String mode) {
        return diagnosticService.findByPk(deviceId, offset, limit, mode);
    }

    /**
     * This method will return list interface avaiable in device for diagnostic
     *
     * @param deviceId
     * @return
     */
    @GET
    @Path("/getInterfaceList")
    public AcsResponse getListInterfaceAvaiable(@QueryParam("deviceId") String deviceId) {
        // create json input
        AcsResponse response = new AcsResponse();
        // neu refresh object thanh cong thi thuc hien lay du lieu tu database genies
        Map<String, String> uriVariable = new HashMap<String, String>();
        uriVariable.put("query", "{\"_id\":\"" + deviceId + "\"}");
        uriVariable.put("projection", Diagnostic.PING_INTERFACE_LIST);
        ResponseEntity<String> responseEntity = this.acsClient.search("devices", uriVariable);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        //total of search result is in http header [total]
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("total").get(0));
        // parse to list interface
        if (responseEntity.getBody() != null)
            response.body = Diagnostic.getListInterface(responseEntity.getBody()).toString();
        return response;
    }
}
