package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.dto.AcsResponse;

import javax.ws.rs.*;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by thangnc on 14-Jun-17.
 */
@Component
@Path("rpc")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("RPC")
public class RPCEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RPCEndpoint.class);

    @Autowired
    private AcsClient acsClient;

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
                              Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = true;
        String commandKey = null;
        try {
            if (request.get("now") != null) {
                now = Boolean.valueOf((String) request.get("now"));
            }
            if (request.get("commandKey") != null) {
                commandKey = (String) request.get("commandKey");
            }
            ResponseEntity<String> responseEntity = acsClient.createRebootTask(deviceId, commandKey, now, null);
            response.httpResponseCode = responseEntity.getStatusCode().value();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    /**
     * Gets parameter names for a device
     * Ask ACS to execute a task to get values for given parameters
     *
     * @param deviceId id of device in ACS
     * @param request  a map containing keys "now" and "parameters"
     * @return AcsResponse object
     */
    @POST
    @Path("/{deviceId}/get-parameter-names")
    public AcsResponse getParameterValues(@PathParam("deviceId") String deviceId,
                                          Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = true;
        Boolean nextLevel = true;
        String parameterPath = null;
        try {
            if (request.get("now") != null) {
                now = (Boolean) request.get("now");
            }
            if (request.get("nextLevel") != null) {
                nextLevel = Boolean.valueOf((String) request.get("nextLevel"));
            }
            if (request.get("parameterPath") != null) {
                parameterPath = (String) request.get("parameterPath");
            }
            ResponseEntity<String> responseEntity = this.acsClient.getParameterNames(deviceId, parameterPath, nextLevel, now);
            response.httpResponseCode = responseEntity.getStatusCodeValue();
            response.body = responseEntity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    @POST
    @Path("/{deviceId}/downloadFile")
    public AcsResponse downloadFile(@PathParam("deviceId") String deviceId,
                                    Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Boolean now = (Boolean) request.get("now");
        String fileType = (String) request.get("fileType");
        String url = (String) request.get("url");
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String successUrl = (String) request.get("successUrl");
        String failureUrl = (String) request.get("failureUrl");
        String commandKey = (String) request.get("commandKey");
        int fileSize = Integer.parseInt((String) request.get("fileSize"));
        String targetFileName = (String) request.get("targetFileName");
        int delaySeconds = Integer.parseInt((String) request.get("delaySeconds"));
        Boolean status = Boolean.valueOf((String) request.get("status")) ;
        String startTime = (String) request.get("startTime");
        String completeTime = (String) request.get("completeTime");
        try {
        ResponseEntity<String> entity = this.acsClient.createDownloadUrlFileTask(deviceId, fileType, url, username, password,
                successUrl, failureUrl, commandKey, fileSize, targetFileName, delaySeconds, status, startTime, completeTime, now);
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        } catch (RestClientException e) {
            response.httpResponseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return response;
    }

    @POST
    @Path("/{deviceId}/uploadFile")
    public AcsResponse uploadFile(@PathParam("deviceId") String deviceId,
                                  Map<String, Object> request) {
        Boolean now = (Boolean) request.get("now");
        String fileType = (String) request.get("fileType");
        String url = (String) request.get("url");
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        int delaySeconds = Integer.parseInt((String) request.get("delaySeconds"));
        String commandKey = (String) request.get("commandKey");
        ResponseEntity<String> entity = this.acsClient.createUploadFileTask(deviceId, fileType, url, username, password, delaySeconds,
                                         commandKey, now, null);
        AcsResponse response = new AcsResponse();
        response.httpResponseCode = entity.getStatusCodeValue();
        response.body = entity.getBody();
        return response;
    }

}
