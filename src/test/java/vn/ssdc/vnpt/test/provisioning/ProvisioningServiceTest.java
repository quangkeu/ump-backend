package vn.ssdc.vnpt.test.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Admin on 4/12/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class ProvisioningServiceTest {

    @Autowired
    RepositoryFactory repositoryFactory;

    private AcsClient acsClient = mock(AcsClient.class);
    private DeviceTypeService deviceTypeService = mock(DeviceTypeService.class);
    private DeviceTypeVersionService deviceTypeVersionService = mock(DeviceTypeVersionService.class);
    private TagService tagService = mock(TagService.class);
    private SubscriberDeviceService subscriberDeviceService = mock(SubscriberDeviceService.class);

    @Test
    public void createProvisioningTasks() throws Exception {

        //Create Device
        String result = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/a06518-968380GERG-VNPT00a532c2.txt")));
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"a06518-968380GERG-VNPT00a532c2\"}");
        ResponseEntity<String> testResult = new ResponseEntity<String>(result, HttpStatus.OK);
        when(acsClient.search("devices", acsQuery)).thenReturn(testResult);
        //Create Device Type
        DeviceType deviceType = new DeviceType();
        deviceType.id = 5l;
        deviceType.name = "96318REF_P300_Broadcom_a06518";
        deviceType.productClass = "968380GERG";
        deviceType.manufacturer = "Broadcom";
        deviceType.oui = "a06518";
        deviceType.modelName = "GW040_2015";
        DeviceTypeService deviceTypeService = new DeviceTypeService(repositoryFactory);
        deviceTypeService.create(deviceType);
        //Create Device Type Version
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.id = 11l;
        deviceTypeVersion.deviceTypeId = 5l;
        deviceTypeVersion.firmwareVersion = "G4.16A.03RC3";
        deviceTypeVersion.modelName = "GW040_2015";
        deviceTypeVersion.oui = "a06518";
        deviceTypeVersion.productClass = "968380GERG";

        String strParamter = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/DeviceTypeVersion_Parameters.txt")));
        HashMap<String,Parameter> parameterHashMap = new ObjectMapper().readValue(strParamter, HashMap.class);
        deviceTypeVersion.parameters = parameterHashMap;

        String strDiagnostics = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/DeviceTypeVersion_Diagnostics.txt")));
        HashMap<String,Tag> diagnosticsHashMap = new ObjectMapper().readValue(strDiagnostics, HashMap.class);
        deviceTypeVersion.diagnostics = diagnosticsHashMap;

        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        deviceTypeVersionService.create(deviceTypeVersion);

        ////Create Tag
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.id = 88l;
        tag.name = "Provisioning for G4";
        tag.assigned = 1;
        tag.deviceTypeVersionId = 11l;
        tag.rootTagId = 87l;

        String tagParam = new String(Files.readAllBytes(Paths.get("src/test/resources/genieacs/Tag_Parameters.txt")));
        HashMap<String,Parameter> tagParamHashmap = new ObjectMapper().readValue(tagParam, HashMap.class);
        tag.parameters = tagParamHashmap;

        tagService.create(tag);
        //
        String strReturn_GetDevice = "[\n" +
                "{\"_id\":\"a06518-968380GERG-VNPT00a532c2\",\"InternetGatewayDevice\":{}}\n" +
                "]";
        ResponseEntity<String>  responseEntity_GetDevice = new ResponseEntity<String>(strReturn_GetDevice, HttpStatus.OK);
        when(acsClient.getDevice("a06518-968380GERG-VNPT00a532c2",
                "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig.5.Enable"))
                .thenReturn(responseEntity_GetDevice);

        //
        ProvisioningService provisioningService = new ProvisioningService(repositoryFactory);
        provisioningService.acsClient = acsClient;
        provisioningService.deviceTypeService = deviceTypeService;
        provisioningService.tagService = tagService;
        provisioningService.deviceTypeVersionService = deviceTypeVersionService;
        //
        provisioningService.createProvisioningTasks("a06518-968380GERG-VNPT00a532c2");
    }

    @Test
    public void getProvisioningValue() throws Exception {
        ProvisioningService provisioningService = new ProvisioningService(repositoryFactory);
        provisioningService.subscriberDeviceService = subscriberDeviceService;
        String strDeviceId = "a06518-968380GERG-VNPT00a532c2";


        List<Subscriber> lstSubscribers = new ArrayList<Subscriber>();
        Subscriber subscriber = new Subscriber();

        Map<String, String> mapData = new HashMap<String, String>();
        mapData.put("subscriberDataKey", "Test_1");
        subscriber.subscriberData = mapData;

        lstSubscribers.add(subscriber);

        when(provisioningService.subscriberDeviceService.findByDeviceId("a06518-968380GERG-VNPT00a532c2"))
                .thenReturn(lstSubscribers);


        Parameter parameter = new Parameter();
        parameter.useSubscriberData = 1;
        parameter.subscriberData = "subscriberDataKey";

        String strReturn = provisioningService.getProvisioningValue(strDeviceId,parameter);
        Assert.assertNotNull(strReturn);
    }


}