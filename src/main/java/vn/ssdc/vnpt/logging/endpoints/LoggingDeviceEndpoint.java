package vn.ssdc.vnpt.logging.endpoints;

import io.searchbox.client.JestClient;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.common.model.Configuration;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.logging.model.LoggingDevice;
import vn.ssdc.vnpt.logging.services.LoggingDeviceService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/device")
@Api("DeviceLog")
@Produces(APPLICATION_JSON)
public class LoggingDeviceEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    LoggingDeviceService loggingDeviceService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    @GET
    @Path("/get-page")
    public List<LoggingDevice> getPageLoggingDevice(@DefaultValue("1") @QueryParam("page") int page,
                                                    @DefaultValue("20") @QueryParam("limit") int limit,
                                                    @DefaultValue("") @QueryParam("name") String name,
                                                    @DefaultValue("") @QueryParam("actor") String actor,
                                                    @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                    @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                                                    @DefaultValue("") @QueryParam("username") String username) {

        if ("acs".equals(actor.toLowerCase())) {
            actor = ""; // Search all if actor is ACS
        }
        return loggingDeviceService.getPage(page, limit, name, actor, fromDateTime, toDateTime, username);
    }

    @GET
    @Path("/get-total-pages")
    public Long getTotalPages(@DefaultValue("1") @QueryParam("page") int page,
                              @DefaultValue("20") @QueryParam("limit") int limit,
                              @DefaultValue("") @QueryParam("name") String name,
                              @DefaultValue("") @QueryParam("actor") String actor,
                              @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                              @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                              @DefaultValue("") @QueryParam("username") String username) {

        if ("acs".equals(actor.toLowerCase())) {
            actor = ""; // Search all if actor is ACS
        }
        return loggingDeviceService.getTotalPages(page, limit, name, actor, fromDateTime, toDateTime, username);
    }


    @POST
    @Path("/remove-all")
    public Boolean removeAllLoggingDevice(@DefaultValue("") @QueryParam("name") String name,
                                          @DefaultValue("") @QueryParam("actor") String actor,
                                          @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                          @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {

        return loggingDeviceService.removeAllElk(name, actor, fromDateTime, toDateTime);
    }

    @POST
    @Path("/post-save-time-expire")
    public Boolean postSaveTimeExpire(Map<String, String> request) {

        Boolean result = false;
        try {
            String timeExpire = request.containsKey("timeExpire") ? request.get("timeExpire") : "";
            Configuration configuration = configurationService.get("timeExpire");
            configuration.value = timeExpire;
            configurationService.update(configuration.id, configuration);
            result = true;

        } catch (Exception e) {
            logger.error("postSaveTimeExpire", e);
        }

        return result;
    }

    @GET
    @Path("/export/{session}")
    public Response exportDataModelXML(@PathParam("session") String session) {
        String path = loggingDeviceService.exportXML(session);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

}
