package vn.ssdc.vnpt.rabbitmq.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.endpoints.AcsEndpoint;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.*;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.vnpt.ssdc.event.AMQPSubscribes;
import vn.vnpt.ssdc.event.Event;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by THANHLX on 5/26/2017.
 */
@Component
@Path("cwmpmq")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Cwmp MQ")
public class CwmpEndPoint
{
    private static final Logger logger = LoggerFactory.getLogger(CwmpEndPoint.class);
    @Autowired
    private AcsClient acsClient;

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private DiagnosticService diagnosticService;

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private BlackListDeviceService blackListDeviceService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private TagService tagService;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @POST
    @Path("/{deviceId}/newDeviceRegistered")
    public void newDeviceRegistered(@PathParam("deviceId") String deviceId) {
        processNewDeviceRegistered(deviceId);
    }

    @AMQPSubscribes(queue = "new-device-registered", concurrency = 64)
    public void processMessageNewDeviceRegistered(Event event) {
        logger.info("New device registered #{}", event.message.get("deviceId"));
        String deviceId = event.message.get("deviceId");
        processNewDeviceRegistered(deviceId);
    }

    public void processNewDeviceRegistered(String deviceId) {
        if(blackListDeviceService.findByDeviceId(deviceId).size() == 0) {
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
        }
        else{
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
                    //Set get list available interface
                    listObjects.add("InternetGatewayDevice.Layer2Bridging.AvailableInterface");
                    if (lTag != null && lTag.size() != 0) {
                        for (int index = 0; index < lTag.size(); index++) {
                            Map<String, Parameter> parameters = lTag.get(index).parameters;
                            for (Map.Entry<String, Parameter> tmp : parameters.entrySet()) {
                                Parameter parameter = new Gson().fromJson(new Gson().toJson(tmp.getValue()), Parameter.class);
                                if(!"object".equals(parameter.dataType)) {
                                    if (parameter.tr069Name.contains("{i}")) {
                                        String parentPath = parameter.tr069ParentObject.substring(0, parameter.tr069ParentObject.indexOf("{i}"));;
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
                                    String parameterPath = path.substring(0,(indexOf+1));
                                    if(!listParameterPaths.contains(parameterPath)){
                                        listParameterPaths.add(parameterPath);
                                    }
                                    indexOf = path.indexOf(".", indexOf + 1);
                                }
                            }
                            for (String path : listParameters) {
                                int indexOf = path.indexOf(".");
                                while (indexOf >= 0) {
                                    String parameterPath = path.substring(0,(indexOf+1));
                                    if(!listParameterPaths.contains(parameterPath)){
                                        listParameterPaths.add(parameterPath);
                                    }
                                    indexOf = path.indexOf(".", indexOf + 1);
                                }
                            }
                            for (String path : listParameterPaths) {
//                                acsClient.getParameterNames(deviceId, path, true,false);
                            }
                            for (String path : listObjects) {
                                acsClient.refreshObject(deviceId, path, false);
                            }
                            if(listParameters.size()>0){
                                acsClient.getParameterValues(deviceId, listParameters, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @AMQPSubscribes(queue = "completed-task", concurrency = 64)
    public void processMessageCompletedTask(Event event) {
        logger.info("Completed task {} for #{}",new Object[] {event.message.get("taskName"),event.message.get("deviceId")});
        Map<String,String> message = event.message;
        if ("refreshAll".equals(message.get("taskName"))) {
            dataModelService.addDataModel(message.get("deviceId"));
            provisioningService.createProvisioningTasks(message.get("deviceId"));
        } else {
            if ("updateDiagnosticResult".equals(message.get("taskName"))) {
                Long diagnosticTaskId = Long.valueOf(message.get("diagnosticTaskId")).longValue();
                diagnosticService.updateResult(diagnosticTaskId);
            }
        }
    }

    @AMQPSubscribes(queue = "fault-task")
    public void processMessageFaultTask(Event event) {
        logger.info("Fault task {} for #{}",new Object[] {event.message.get("taskName"),event.message.get("deviceId")});
        Map<String,String> message = event.message;
        if ("createDiagnostic".equals(message.get("taskName"))) {
            DiagnosticTask diagnosticTask = diagnosticService.findByTaskId(message.get("taskId"));
            diagnosticTask.status = 2;
            diagnosticService.update(diagnosticTask.id, diagnosticTask);
        }
    }

    @AMQPSubscribes(queue = "inform", concurrency = 64)
    public void processMessageInform(Event event) {
        String deviceId = event.message.get("deviceId");
        logger.info("Inform for #{}", new Object[]{deviceId});
        Map<String, String> message = event.message;
        String[] eventCodes = new Gson().fromJson(message.get("eventCodes"), String[].class);
        for(String eventCode : eventCodes){
            if("0 BOOTSTRAP".equals(eventCode)){
                synchronizeDevice(deviceId);
                provisioningService.createProvisioningTasks(deviceId);
            }
            else if("8 DIAGNOSTICS COMPLETE".equals(eventCode)){
                List<DiagnosticTask> taskList = diagnosticService.findInProcess();
                for(DiagnosticTask task : taskList){
                    List<String> listPath = new ArrayList<String>();
                    for (Map.Entry<String, Parameter> entry : task.parameterFull.entrySet())
                    {
                        listPath.add(entry.getValue().path);
                    }
                    this.acsClient.updateDiagnosticResult(task.deviceId,task.id,listPath,true);
                }
            }
        }
    }
}
