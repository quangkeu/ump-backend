package vn.ssdc.vnpt.logging.endpoints;

import io.searchbox.client.JestClient;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyTask;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/policy")
@Api("PolicyLog")
@Produces(APPLICATION_JSON)
public class LoggingPolicyEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoggingPolicyEndpoint.class);

    @Autowired
    LoggingPolicyService loggingPolicyService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    @GET
    @Path("/get-page")
    public List<PolicyTask> getPageLoggingDevice(@DefaultValue("1") @QueryParam("page") int page,
                                                 @DefaultValue("20") @QueryParam("limit") int limit,
                                                 @DefaultValue("0") @QueryParam("policyJobId") Long policyJobId,
                                                 @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                 @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {

        return loggingPolicyService.getPage(page, limit, policyJobId);
    }

    @GET
    @Path("/get-summary")
    public Map<String, Long> getSummary(@DefaultValue("0") @QueryParam("policyJobId") Long policyJobId) {
        return loggingPolicyService.getSummary(policyJobId);
    }


    @POST
    @Path("/remove-all")
    public Boolean removeAllLoggingDevice() {
        return loggingPolicyService.removeAllElk();
    }

}
