package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.*;

import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Created by Lamborgini on 3/30/2017.
 */
@Component
@Path("files")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Files")
public class FileEndpoint {

    @Autowired
    private AcsClient acsClient;

    @POST
    @Path("/search-file/{query}/{limit}/{offset}/{parameters}")
    public AcsResponse searchFile(@DefaultValue("") @PathParam("query") String query,
                                  @PathParam("offset") String offset,
                                  @PathParam("limit") String limit,
                                  @PathParam("parameters") String parameters) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        if (query.startsWith("{")) {
            queryParams.put("query", query);
        } else {
            queryParams.put("query", "{" + query + "}");
        }
        queryParams.put("limit", limit);
        queryParams.put("sort", "{\"uploadDate\":-1}");
        queryParams.put("skip", offset);
        if (!ObjectUtils.empty(parameters)) {
            queryParams.put("projection", parameters);
        }
        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));

        return response;
    }

    @POST
    @Path("/update-file")
    public AcsResponse updateFile(Map<String, Object> request) {
        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();

        queryParams.put("update", "{\"_id\" : ObjectId(\"593771aeddcfbe56abd9279d\")},{$set: {\"metadata.oui\": \"Ahihi\"}}");

        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        response.body = responseEntity.getBody();
        response.nbOfItems = Integer.parseInt(responseEntity.getHeaders().get("totalAll").get(0));

        return response;
    }

    @POST
    @Path("/delete-file/{paramId}/{paramName}")
    public String deleteFile(@PathParam("paramId") String paramId,
                             @PathParam("paramName") String paramName) {
        return this.acsClient.deleteDevice("files", paramId, paramName);
    }

    @POST
    @Path("/check-by-version")
    public boolean checkByVersion(Map<String, Object> request) {

        AcsResponse response = new AcsResponse();
        Map<String, String> queryParams = new HashMap<String, String>();
        StringBuilder sbOR = new StringBuilder("{\"$or\":[");
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            ArrayList<LinkedHashMap> value = (ArrayList<LinkedHashMap>) entry.getValue();
            LinkedHashMap lhm = value.get(0);
            Set set = lhm.entrySet();
            Iterator iterator = set.iterator();
            StringBuilder sbAND = new StringBuilder(",{");
            while (iterator.hasNext()) {
                Map.Entry me = (Map.Entry) iterator.next();

                if (me.getKey().equals("oui")) {
                    sbAND.append(String.format("\"metadata.oui\":\"%s\"", me.getValue()));
                }
                if (me.getKey().equals("productClass")) {
                    sbAND.append(String.format(",\"metadata.productClass\":\"%s\"", me.getValue()));
                }
                if (me.getKey().equals("version")) {
                    sbAND.append(String.format(",\"metadata.version\":\"%s\"", me.getValue()));
                }
            }
            sbAND.append("}");
            sbOR.append(sbAND.toString());
        }

        sbOR.deleteCharAt(8);
        sbOR.append("]}");
        queryParams.put("query", sbOR.toString());
        queryParams.put("projection", "_id");

        ResponseEntity<String> responseEntity = this.acsClient.searchFile("files", queryParams);
        response.httpResponseCode = responseEntity.getStatusCodeValue();
        if (responseEntity.getBody().contains("_id")) {
            return false;
        }

        return true;
    }
}
