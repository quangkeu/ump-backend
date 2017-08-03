package vn.ssdc.vnpt.provisioning.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by SSDC on 11/30/2016.
 */
@Component
@Path("provisioning")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Provisioning")
public class ProvisioningEndPoint {

    private static final Logger logger = LoggerFactory.getLogger(ProvisioningEndPoint.class);
    private ProvisioningService provisioningService;

    @GET
    @Path("/createProvisioningTasks/{deviceId}")
    public void getRootTagByDeviceTypeVersionId(@PathParam("deviceId") String deviceId) {
        provisioningService.createProvisioningTasks(deviceId);
    }
}
