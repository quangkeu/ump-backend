package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.BlacklistDevice;
import vn.ssdc.vnpt.devices.services.BlackListDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Lamborgini on 3/3/2017.
 */
@Component
@Path("blacklist-devices")
@Api("BlacklistDevices")
@Produces(APPLICATION_JSON)
public class BlackListDeviceEndpoint extends SsdcCrudEndpoint<Long, BlacklistDevice> {

    private BlackListDeviceService blackListDeviceService;

    @Autowired
    public BlackListDeviceEndpoint(BlackListDeviceService blackListDeviceService) {
        this.service = this.blackListDeviceService = blackListDeviceService;
    }
}
