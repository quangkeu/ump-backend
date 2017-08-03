package vn.ssdc.vnpt.test.devices;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Lamborgini on 4/13/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestDeviceTypeVersionService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void testGetPage() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.deviceTypeId = 22L;
        deviceTypeVersionService.create(deviceTypeVersion);
        Page<DeviceTypeVersion> deviceTypeVersionPage = deviceTypeVersionService.getPage(1, 20);
        Assert.assertNotNull(deviceTypeVersionPage.getNumber());
    }

    @Test
    public void testFindByManufacturerAndModelName() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.manufacturer = "Broadcom";
        deviceTypeVersion.modelName = "GW040_2015";
        deviceTypeVersionService.create(deviceTypeVersion);
        List<DeviceTypeVersion> deviceTypeVersionList = deviceTypeVersionService.findByManufacturerAndModelName("Broadcom", "GW040_2015");
        Assert.assertNotNull(deviceTypeVersionList.get(0).id);
    }

    @Test
    public void testCountDeviceTypeIDForSortAndSearch() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.manufacturer = "Broadcom";
        deviceTypeVersion.modelName = "GW040_2015";
        deviceTypeVersionService.create(deviceTypeVersion);
        int i = deviceTypeVersionService.countDeviceTypeIDForSortAndSearch("Broadcom", "GW040_2015", "created:-1", "20", "1");
        Assert.assertNotNull(i);
    }

    @Test
    public void testGetDeviceTypeIDForSortAndSearch() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.manufacturer = "Broadcom";
        deviceTypeVersion.modelName = "GW040_2016";
        deviceTypeVersionService.create(deviceTypeVersion);
        List<DeviceTypeVersion> deviceTypeIDForSortAndSearch = deviceTypeVersionService.getDeviceTypeIDForSortAndSearch("Broadcom", "GW040_2016", "created:-1", "20", "0");
        Assert.assertNotNull(deviceTypeIDForSortAndSearch.get(0).id);
    }

    @Test
    public void testSearchDevices() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.deviceTypeId = 25L;
        deviceTypeVersionService.create(deviceTypeVersion);
        List<DeviceTypeVersion> deviceTypeIDForSortAndSearch = deviceTypeVersionService.searchDevices( "20", "0","25");
        Assert.assertNotNull(deviceTypeIDForSortAndSearch.get(0).id);
    }

//    @Test
//    public void testFindbyDevice() {
//        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
//        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
//        deviceTypeVersion.deviceTypeId = 29L;
//        deviceTypeVersionService.create(deviceTypeVersion);
//        DeviceTypeVersion deviceTypeIDForSortAndSearch = deviceTypeVersionService.findbyDevice( "29");
//        Assert.assertNotNull(deviceTypeIDForSortAndSearch.id);
//    }

    @Test
    public void testFindByManufacturer() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.manufacturer = "Broadcom";
        deviceTypeVersionService.create(deviceTypeVersion);
        List<DeviceTypeVersion> deviceTypeIDForSortAndSearch = deviceTypeVersionService.findByManufacturer( "Broadcom");
        Assert.assertNotNull(deviceTypeIDForSortAndSearch.get(0).id);
    }

    @Test
    public void testFindByFirmwareVersion() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.firmwareVersion = "G4.16A.03RC4";
        deviceTypeVersionService.create(deviceTypeVersion);
        DeviceTypeVersion byFirmwareVersion = deviceTypeVersionService.findByFirmwareVersion("G4.16A.03RC4");
        Assert.assertNotNull(byFirmwareVersion.id);
    }

    @Test
    public void testFindByPk() {
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.deviceTypeId = 27L;
        deviceTypeVersion.firmwareVersion = "G4.16A.03RC3";
        deviceTypeVersion.parameters = new HashMap<String, Parameter>();
        deviceTypeVersion.created = System.currentTimeMillis();
        deviceTypeVersion.updated = System.currentTimeMillis() - deviceTypeVersion.created;
        deviceTypeVersion.diagnostics = new HashMap<String, Tag>();
        deviceTypeVersion.manufacturer = "Broadcom";
        deviceTypeVersion.oui = "a06518";
        deviceTypeVersion.productClass = "968380GERG";
        deviceTypeVersion.modelName = "GW040_2017";
        deviceTypeVersionService.create(deviceTypeVersion);
        DeviceTypeVersion byFirmwareVersion = deviceTypeVersionService.findByPk(27L,"G4.16A.03RC3");
        Assert.assertNotNull(byFirmwareVersion.id);
    }

//    @Test
//    public void testPrev() {
//        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService(repositoryFactory);
//        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
//        deviceTypeVersion.deviceTypeId = 26L;
//        deviceTypeVersion.firmwareVersion = "G4.16A.03RC3";
//        deviceTypeVersion.dataModelFileName = "";
//        deviceTypeVersion.firmwareFileName = "";
//        deviceTypeVersion.firmwareFileId = "";
//        deviceTypeVersion.parameters = new HashMap<String, Parameter>();
//        deviceTypeVersion.created = System.currentTimeMillis();
//        deviceTypeVersion.updated = System.currentTimeMillis() - deviceTypeVersion.created;
//        deviceTypeVersion.diagnostics = new HashMap<String, Tag>();
//        deviceTypeVersion.manufacturer = "Broadcom";
//        deviceTypeVersion.oui = "a06518";
//        deviceTypeVersion.productClass = "968380GERG";
//        deviceTypeVersion.modelName = "GW040_2017";
//        deviceTypeVersionService.create(deviceTypeVersion);
//        DeviceTypeVersion byFirmwareVersion = deviceTypeVersionService.prev(26L);
//        Assert.assertNotNull(byFirmwareVersion.id);
//    }
}
