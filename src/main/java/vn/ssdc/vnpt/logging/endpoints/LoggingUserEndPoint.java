package vn.ssdc.vnpt.logging.endpoints;

import io.searchbox.client.JestClient;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.logging.model.LoggingUser;
import vn.ssdc.vnpt.logging.services.LoggingUserService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/user")
@Api("UserLog")
@Produces(APPLICATION_JSON)
public class LoggingUserEndPoint extends SsdcCrudEndpoint<Long, LoggingUser> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    private LoggingUserService loggingUserService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Autowired
    public LoggingUserEndPoint(LoggingUserService loggingUserService) {
        this.service = this.loggingUserService = loggingUserService;
    }

    @GET
    @Path("/get-page")
    public Page<LoggingUser> getPage(@DefaultValue("0") @QueryParam("page") int page,
                                     @DefaultValue("20") @QueryParam("limit") int limit,
                                     @DefaultValue("") @QueryParam("name") String name,
                                     @DefaultValue("") @QueryParam("actor") String actor,
                                     @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                     @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingUserService.getPage(page, limit, name, actor, fromDateTime, toDateTime);
    }

    @POST
    @Path("/remove-all")
    public Boolean removeAll(@DefaultValue("") @QueryParam("name") String name,
                             @DefaultValue("") @QueryParam("actor") String actor,
                             @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                             @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingUserService.removeAllElk(name, actor, fromDateTime, toDateTime);
    }

    @GET
    @Path("/get-update-mysql")
    public List<LoggingUser> getPage(@DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                     @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingUserService.getUpdateMysql(fromDateTime, toDateTime);
    }

}
