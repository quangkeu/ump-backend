package vn.ssdc.vnpt.common.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.common.model.EmailTemplate;
import vn.ssdc.vnpt.common.services.EmailTemplateService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Lamborgini on 6/7/2017.
 */
@Component
@Path("email-template")
@Api("EmailTemplate")
@Produces(APPLICATION_JSON)
public class EmailTemplateEndPoint  extends SsdcCrudEndpoint<String, EmailTemplate> {

    private EmailTemplateService emailTemplateService;

    @Autowired
    public EmailTemplateEndPoint(EmailTemplateService emailTemplateService) {
        this.service = this.emailTemplateService = emailTemplateService;
    }

    @GET
    @Path("/search-email")
    public List<EmailTemplate> searchRole(
            @DefaultValue("20") @QueryParam("limit") String limit,
            @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        return this.emailTemplateService.searchEmail(limit,indexPage);
    }
}
