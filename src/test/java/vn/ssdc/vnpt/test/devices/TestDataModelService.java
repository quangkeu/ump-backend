package vn.ssdc.vnpt.test.devices;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by THANHLX on 4/13/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestDataModelService {
    @Autowired
    RepositoryFactory repositoryFactory;

    private AcsClient acsClient = mock(AcsClient.class);

    @Test
    public void testGetInforDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{\"_id\":\"a06518-968380GERG-VNPT00a532c2\"}");
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.search("devices", acsQuery)).thenReturn(testResult);

            DataModelService dataModelService = new DataModelService(repositoryFactory);
            dataModelService.setAcsClient(acsClient);
            JsonObject jsonObject = dataModelService.getInforDevice("a06518-968380GERG-VNPT00a532c2");
            Assert.assertNotNull(jsonObject);
            Assert.assertEquals("a06518-968380GERG-VNPT00a532c2",jsonObject.get("_id").getAsString());
        }
        catch (IOException e){
            Assert.fail("Exception " + e);
        }
    }
}
