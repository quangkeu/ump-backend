package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("parameter-detail")
@Produces(APPLICATION_JSON)
@Api("ParameterDetails")
public class ParameterDetailEndpoint extends SsdcCrudEndpoint<Long, ParameterDetail> {

    private ParameterDetailService parameterDetailService;

    @Autowired
    public ParameterDetailEndpoint(ParameterDetailService parameterDetailService) {
        this.service = this.parameterDetailService = parameterDetailService;
    }

    @GET
    @Path("/find-by-device-type-version/{deviceTypeVersionId}")
    public List<ParameterDetail> findByDeviceTypeVersion(@PathParam("deviceTypeVersionId") Long deviceTypeVersionId) {
        return parameterDetailService.findByDeviceTypeVersion2(deviceTypeVersionId);
    }

    @GET
    @Path("/find-by-params")
    public ParameterDetail findByDeviceTypeVersion(@QueryParam("deviceTypeVersionId") Long deviceTypeVersionId,
                                                   @QueryParam("path") String path) {
        return parameterDetailService.findByParams(path, deviceTypeVersionId);
    }

    @GET
    @Path("/find-parameters")
    public List<ParameterDetail> findParameters() {
        return parameterDetailService.findParameters();
    }

    @GET
    @Path("/find-by-tr069-name")
    public List<ParameterDetail> findByTr069Name(@QueryParam("tr069Name") String tr069Name) {
        return parameterDetailService.findByTr069name(tr069Name);
    }

}
