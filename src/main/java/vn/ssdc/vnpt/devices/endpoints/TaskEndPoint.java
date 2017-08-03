package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.dto.AcsResponse;

import javax.ws.rs.*;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by SSDC on 11/11/2016.
 */

@Component
@Path("tasks")
@Api("Tasks")
@Produces(APPLICATION_JSON)
public class TaskEndPoint {

    private static final String DEVICE_ID_FIELD = "device";
    private static final String DEVICE_METHOD_FIELD = "name";

    private static final String DEVICE_TOTAL = "total";
    private static final String DEVICE_QUERY = "query";
    private static final String DEVICE_TASK = "tasks";

    @Autowired
    private AcsClient acsClient;


    /**
     * return a list tasks belong to deviceId in response body
     *
     * @param deviceId
     * @return
     */
    @GET
    @Path("/{deviceId}")
    public AcsResponse findTaskByDeviceId(@PathParam("deviceId") String deviceId) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(DEVICE_QUERY, "{\"" + DEVICE_ID_FIELD + "\":\"" + deviceId + "\"}");
        ResponseEntity<String> responseEntity = this.acsClient.search(DEVICE_TASK, queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        //total of search result is in http header [total]
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get(DEVICE_TOTAL).get(0));
        response.body = responseEntity.getBody();
        return response;
    }

    /**
     * return a list tasks belong to deviceId in response body
     *
     * @param deviceId
     * @return
     */
    @GET
    @Path("/{deviceId}/newest/{methodname}")
    public AcsResponse findTaskByDeviceIdNewEst(@PathParam("deviceId") String deviceId, @PathParam("methodname") String methodname) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(DEVICE_QUERY, "{\"" + DEVICE_ID_FIELD + "\":\"" + deviceId + "\",\"" + DEVICE_METHOD_FIELD + "\":\"" + methodname + "\"}");
        queryParams.put("limit", "1");
        queryParams.put("sort", "{\"" + "timestamp" + "\":" + -1 + "}");
        ResponseEntity<String> responseEntity = this.acsClient.search(DEVICE_TASK, queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        //total of search result is in http header [total]
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get(DEVICE_TOTAL).get(0));
        response.body = responseEntity.getBody();
        return response;
    }

    @GET
    @Path("/checkExist/{taskId}")
    public AcsResponse checkTaskExist(@PathParam("taskId") String taskId) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(DEVICE_QUERY, "{\"" + "_id" + "\":\"" + taskId + "\"}");
        queryParams.put("limit", "1");
        ResponseEntity<String> responseEntity = this.acsClient.search(DEVICE_TASK, queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        //total of search result is in http header [total]
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get(DEVICE_TOTAL).get(0));
        response.body = responseEntity.getBody();
        return response;
    }


    /**
     * this method will delete task which has taskId
     *
     * @param taskId
     */
    @DELETE
    @Path("/{taskId}")
    public void deleteTask(@PathParam("taskId") String taskId) {
        acsClient.deleteTask(taskId);
    }

    /**
     * this method will retry task which has taskId
     *
     * @param taskId
     * @return
     */
    @POST
    @Path("/retry/{taskId}")
    public ResponseEntity<String> retryTask(@PathParam("taskId") String taskId) {
        return acsClient.retryTask(taskId);
    }

}
