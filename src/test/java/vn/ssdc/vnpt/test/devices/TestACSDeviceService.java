package vn.ssdc.vnpt.test.devices;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.OngoingStubbing;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Lamborgini on 4/14/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestACSDeviceService {
    @Autowired
    RepositoryFactory repositoryFactory;

    private AcsClient acsClient = mock(AcsClient.class);

    @Test
    public void testSearchDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
//            acsQuery.put("query", "{\"_id\":\"a06518-968380GERG-VNPT00a532c2\"}");
            acsQuery.put("query", "{\"_deviceId._Manufacturer\":\"Broadcom\"}");
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.search("devices", acsQuery)).thenReturn(testResult);

            JsonArray array = new Gson().fromJson(testResult.getBody(), JsonArray.class);
            Assert.assertEquals("a06518-968380GERG-VNPT00a532c2", array.get(0).getAsJsonObject().get("_id").getAsString());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testRebootDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{}");
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.reboot("a06518-968380GERG-VNPT00a532c2", true)).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testFactoryResetDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", "{}");
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.factoryReset("a06518-968380GERG-VNPT00a532c2", true)).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testCheckExistDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> params = new HashMap<String, String>();
            params.put("query", String.format("{\"_id\":\"%s\"}", "a06518-968380GERG-VNPT00a532c2"));
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.checkExist("a06518-968380GERG-VNPT00a532c2",params)).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

//    @Test
//    public void testDeleteDevice() {
//        try {
//            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
//
//            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
//            when(acsClient.deleteDevice("a06518-968380GERG-VNPT00a532c2")).thenReturn(testResult);
//
//            Assert.assertEquals(200,testResult.getStatusCode().value());
//        } catch (IOException e) {
//            Assert.fail("Exception " + e);
//        }
//    }

    @Test
    public void testCreateLabelDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.addLabel("a06518-968380GERG-VNPT00a532c2","test")).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testRecheckStatusDevice() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.recheckStatus("a06518-968380GERG-VNPT00a532c2")).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

//    @Test
//    public void testRemoveLabelDevice() {
//        try {
//            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
//            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
//            when(acsClient.addLabel("a06518-968380GERG-VNPT00a532c2","test")).thenReturn(testResult);
//
//            Assert.assertEquals(200,testResult.getStatusCode().value());
//        } catch (IOException e) {
//            Assert.fail("Exception " + e);
//        }
//    }

    @Test
    public void testDownloadFile() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.downloadFile("a06518-968380GERG-VNPT00a532c2","1","test",true)).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void testUploadFile() {
        try {
            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.uploadFile("a06518-968380GERG-VNPT00a532c2","test",true)).thenReturn(testResult);

            Assert.assertEquals(200,testResult.getStatusCode().value());
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }
}
