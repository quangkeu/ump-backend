package vn.ssdc.vnpt.subscriber.endpoints;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Huy Hieu on 11/25/2016.
 */

@Component
@Path("subscriber-devices")
@Api("Subscribers")
@Produces(APPLICATION_JSON)
public class SubscriberDeviceEndpoint extends SsdcCrudEndpoint<Long, SubscriberDevice> {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberDeviceEndpoint.class);

    private SubscriberDeviceService subscriberDeviceService;

    public SubscriberDeviceEndpoint(SubscriberDeviceService subscriberDeviceService) {
        this.service = subscriberDeviceService;
        this.subscriberDeviceService = subscriberDeviceService;
    }

    @GET
    @Path("/find-by-subscriber-id")
    public List<SubscriberDevice> findBySubscribeId(@QueryParam("subscriberId") String subscriberId) {
        return subscriberDeviceService.findBySubscribeId(subscriberId);
    }

    @GET
    @Path("/replace-cpe/{oldDeviceId}/{newDeviceId}")
    public boolean replaceCPE(@PathParam("oldDeviceId") String oldDeviceId,
                              @PathParam("newDeviceId") String newDeviceId) {
        return this.subscriberDeviceService.replaceCPE(oldDeviceId, newDeviceId);
    }

    @GET
    @Path("/find-by-device-id")
    public boolean findSubByDeviceId(@QueryParam("deviceId") String deviceId) {
        return subscriberDeviceService.findSubByDeviceId(deviceId);
    }
}
