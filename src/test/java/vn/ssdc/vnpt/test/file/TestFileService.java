package vn.ssdc.vnpt.test.file;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Lamborgini on 4/13/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestFileService {
    @Autowired
    RepositoryFactory repositoryFactory;

    private AcsClient acsClient = mock(AcsClient.class);

    @Test
    public void testsearchFile() {

        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/ObjectId(5833b35776b5409a28b422dc).txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{\"_id\":\"ObjectId(\"5833b35776b5409a28b422dc\")\"}");
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.searchFile("files", acsQuery)).thenReturn(testResult);

            JsonObject object = new Gson().fromJson(testResult.getBody(), JsonObject.class);
            Assert.assertEquals("ObjectId(\"5833b35776b5409a28b422dc\")", object.get("_id").getAsString());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testCheckByVersion() {

        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/ObjectId(5833b35776b5409a28b422dc).txt")));

            Map<String, String> queryParams = new HashMap<String, String>();
            StringBuilder sbOR = new StringBuilder("{\"$or\":[");
            StringBuilder sbAND = new StringBuilder(",{");
            sbAND.append(String.format("\"metadata.oui\":\"%s\"", "a06518"));
            sbAND.append(String.format(",\"metadata.productClass\":\"%s\"", "968380GERG"));
            sbAND.append(String.format(",\"metadata.version\":\"%s\"", "\"G4.16A.03RC3\""));
            sbAND.append("}");
            sbOR.append(sbAND.toString());

            sbOR.deleteCharAt(8);
            sbOR.append("]}");
            queryParams.put("query", sbOR.toString());
            queryParams.put("projection", "_id");

            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.searchFile("files", queryParams)).thenReturn(testResult);

            JsonObject object = new Gson().fromJson(testResult.getBody(), JsonObject.class);
            Assert.assertEquals("ObjectId(\"5833b35776b5409a28b422dc\")", object.get("_id").getAsString());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

}
