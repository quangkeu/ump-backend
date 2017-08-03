package vn.ssdc.vnpt.policy.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by THANHLX on 4/14/2017.
 */
@Component
@Path("policy-task")
@Api("Policies")
@Produces(APPLICATION_JSON)
public class PolicyTaskEnpoint extends SsdcCrudEndpoint<Long, PolicyTask> {
    private static final Logger logger = LoggerFactory.getLogger(PolicyTaskEnpoint.class);

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    public PolicyTaskEnpoint(PolicyTaskService policyTaskService) {
        this.service = this.policyTaskService = policyTaskService;
    }

    @GET
    @Path("/get-page")
    public List<PolicyTask> getPage(
                                   @QueryParam("policyJobId") Long policyJobId,
                                   @QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("limit") @DefaultValue("20") int limit) {
        return loggingPolicyService.getPage(page, limit, policyJobId);
    }

    @GET
    @Path("/get-page-device-activity")
    public List<LoggingDeviceActivity> getPageDeviceActivity(@DefaultValue("1") @QueryParam("page") int page,
                                                             @DefaultValue("20") @QueryParam("limit") int limit,
                                                             @DefaultValue("") @QueryParam("deviceId") String deviceId,
                                                             @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                             @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {

//        if ("".equals(deviceId)) {
//            return null;
//        }
        return loggingPolicyService.getPageDeviceActivity(page, limit, deviceId, fromDateTime, toDateTime);
    }

    @GET
    @Path("/get-summary-device-activity")
    public Map<String, Long> getSummaryDeviceActivity(@DefaultValue("") @QueryParam("deviceId") String deviceId,
                                                      @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                      @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingPolicyService.getSummaryDeviceActivity(deviceId, fromDateTime, toDateTime);
    }

    @GET
    @Path("/get-summary")
    public Map<String, Long> getSummary(@DefaultValue("0") @QueryParam("policyJobId") Long policyJobId) {
        return loggingPolicyService.getSummary(policyJobId);
    }

    @POST
    @Path("/count")
    public long count(@RequestParam String query) {
        return this.policyTaskService.count(query);
    }

    @GET
    @Path("/find-by-taskId")
    public PolicyTask findByTaskId(@QueryParam("taskId") String taskId) {
        return this.policyTaskService.findByTaskId(taskId);
    }

    @GET
    @Path("/group-by-status")
    public List<PolicyTask> groupByStatus(){
        return this.policyTaskService.groupByStatus();
    }

    @POST
    @Path("/delete-device-activity")
    public Boolean deleteDeviceActivity(Map<String, String> request) {
        Boolean result = false;
        if (request.containsKey("id")) {
            result = loggingPolicyService.removeById(request.get("id"));
        }
        return result;
    }
}
