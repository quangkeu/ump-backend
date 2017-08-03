package vn.ssdc.vnpt.logging.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.DeleteByQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.ssdc.vnpt.logging.model.DeleteByQuery5;

import java.io.IOException;

@Service
public class ElkService {

    @Autowired
    JestClient elasticSearchClient;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    private Integer getVersionElk() {
        Integer result = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(elasticSearchUrl, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String version = root.path("version").path("number").toString().replaceAll("\"", "");
            result = Integer.valueOf(version.split("[.]")[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public Boolean deleteByBoolQuery(BoolQueryBuilder boolQueryBuilder, String elkIndex, String elkType) {
        Boolean result = false;
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            JestResult jestResult;
            if (getVersionElk() >= 5) {
                DeleteByQuery5 deleteByQuery = new DeleteByQuery5.Builder(searchSourceBuilder.toString())
                        .addIndex(elkIndex).addType(elkType).build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            } else {
                DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(searchSourceBuilder.toString())
                        .addIndex(elkIndex).addType(elkType).build();
                jestResult = elasticSearchClient.execute(deleteByQuery);
            }
            result = jestResult.isSucceeded();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
