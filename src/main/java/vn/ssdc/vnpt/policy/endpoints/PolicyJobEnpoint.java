package vn.ssdc.vnpt.policy.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.endpoints.DiagnosticEndPoint;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Admin on 3/3/2017.
 */
@Component
@Path("policy")
@Api("Policies")
@Produces(APPLICATION_JSON)
public class PolicyJobEnpoint extends SsdcCrudEndpoint<Long,PolicyJob> {
    private static final Logger logger = LoggerFactory.getLogger(SsdcCrudEndpoint.class);

    @Autowired
    private PolicyJobService policyJobService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    public PolicyJobEnpoint(PolicyJobService service) {
        this.service = service;
    }

    @POST
    @Path("/{policyJobId}/excute")
    public void excute(@PathParam("policyJobId") Long policyJobId) {
        try {
            //1st update status job
            PolicyJob policyJob = policyJobService.get(policyJobId);
            //2st
            if (policyJob.isImmediately) {
                //1st get all list device
                if(policyJob.deviceGroupId == null){
                    for(String deviceId : policyJob.externalDevices){
                        policyJobService.createTask(deviceId, policyJob);
                    }
                }
                else {
                    List<String> listDevice = deviceGroupService.getListDeviceByGroup(policyJob.deviceGroupId);
                    for (int intIndex = 0; intIndex < listDevice.size(); intIndex++) {
                        String deviceId = listDevice.get(intIndex);
                        policyJobService.createTask(deviceId, policyJob);
                    }
                }
            } else {
                policyJobService.createQuartzJob(policyJob.startAt, policyJobId, policyJob.timeInterval);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    @POST
    @Path("/{policyJobId}/stop")
    public String stop(@PathParam("policyJobId") Long policyJobId) {
        try {
            //1st.Delete Job
            policyJobService.deleteQuartzJob(policyJobId);
            //2st.Delete Trigger
            policyJobService.deleteTriger(policyJobId);
            //3st.Delete Preset
            policyJobService.deletePreset(policyJobId);
            return "DELETE POLICY JOB SUCCESS";
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
            return "DELETE POLICY JOB ERROR";
        }
    }

    @GET
    @Path("/check-job-by-group")
    public Boolean findJobByDeviceGroup(@QueryParam("groupId") String groupId, @QueryParam("status") String status) {
        return this.policyJobService.findJobExecute(groupId, status);
    }

    @GET
    @Path("/get-page")
    public Page<PolicyJob> getPage(@QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("limit") @DefaultValue("20") int limit) {
        return this.policyJobService.getPage(page, limit);
    }

    @GET
    @Path("/get-page-with-number-of-execution")
    public Page<PolicyJob> getPageWithNumberOfExecution(@QueryParam("page") @DefaultValue("0") int page,
                                                        @QueryParam("limit") @DefaultValue("20") int limit) {
        return this.policyJobService.getPageWithNumberOfExecution(page, limit);
    }
}
