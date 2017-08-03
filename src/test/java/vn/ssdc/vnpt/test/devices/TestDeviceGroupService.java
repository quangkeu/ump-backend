package vn.ssdc.vnpt.test.devices;

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
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by thangnc on 13-Apr-17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestDeviceGroupService {

    @Autowired
    RepositoryFactory repositoryFactory;

    private AcsClient acsClient = mock(AcsClient.class);

    @Test
    public void testFindByName() {
        DeviceGroupService deviceGroupService = new DeviceGroupService(repositoryFactory);
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.name = "test";
        deviceGroupService.create(deviceGroup);
        List<DeviceGroup> deviceGroupList = deviceGroupService.findByName("test");
        Assert.assertNotNull(deviceGroupList);
        Assert.assertEquals(deviceGroupList.get(0).name,"test");
    }

    @Test
    public void testFindByPage() {
        DeviceGroupService deviceGroupService = new DeviceGroupService(repositoryFactory);
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.name = "test";
        deviceGroupService.create(deviceGroup);
        List<DeviceGroup> deviceGroupList = deviceGroupService.findByPage(1, 1, String.valueOf(deviceGroup.id));
        Assert.assertNotNull(deviceGroupList);
        Assert.assertEquals(deviceGroupList.size(), 1);
    }

    @Test
    public void testGetListDeviceByGroup() {
        try {
            DeviceGroupService deviceGroupService = new DeviceGroupService(repositoryFactory);
            deviceGroupService.setAcsClient(acsClient);
            DeviceGroup deviceGroup = new DeviceGroup();
            deviceGroup.name = "test";
            deviceGroup.query = "{\"$and\":[{\"_id\":\"a06518-968380GERG-VNPT00a532c2\"}]}";
            long groupID = deviceGroupService.create(deviceGroup).id;

            deviceGroup = deviceGroupService.get(groupID);
            Assert.assertNotNull(deviceGroup);
            Assert.assertEquals(deviceGroup.name,"test");

            String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
            Map<String, String> acsQuery = new HashMap<String, String>();
            acsQuery.put("query", deviceGroup.query);
            ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
            when(acsClient.search("devices", acsQuery)).thenReturn(testResult);

//            List<String> listDeviceByGroup = deviceGroupService.getListDeviceByGroup(groupID);
//            Assert.assertNotNull(listDeviceByGroup);
//            Assert.assertEquals("a06518-968380GERG-VNPT00a532c2",listDeviceByGroup.get(0));
        } catch (IOException e) {
            Assert.fail("Exception " + e);
        }
    }

}
