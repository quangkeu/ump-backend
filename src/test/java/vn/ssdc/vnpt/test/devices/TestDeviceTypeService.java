package vn.ssdc.vnpt.test.devices;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.devices.model.DeviceType;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import javax.ws.rs.NotFoundException;
import java.util.List;

/**
 * Created by Lamborgini on 4/14/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestDeviceTypeService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void testFindByManufacturerAndModelName() {
        DeviceTypeService deviceTypeService = new DeviceTypeService(repositoryFactory);
        DeviceType deviceType = new DeviceType();
        deviceType.manufacturer = "Broadcom";
        deviceType.modelName = "GW040_2015";
        deviceTypeService.create(deviceType);
        List<DeviceType> byManufacturerAndModelName = deviceTypeService.findByManufacturerAndModelName("Broadcom", "GW040_2015");
        Assert.assertNotNull(byManufacturerAndModelName.get(0).id);
    }

    @Test
    public void testIsExisted() {
        DeviceTypeService deviceTypeService = new DeviceTypeService(repositoryFactory);
        DeviceType deviceType = new DeviceType();
        deviceType.manufacturer = "Broadcom";
        deviceType.oui = "a06518";
        deviceType.productClass = "968380GERG";
        deviceTypeService.create(deviceType);
        DeviceType byPk = deviceTypeService.findByPk("Broadcom", "a06518", "968380GERG");
        Assert.assertNotNull(byPk.id);
    }

    @Test
    public void beforeDelete() {
        Boolean pass = false;

        DeviceTypeService deviceTypeService = new DeviceTypeService(repositoryFactory);
        DeviceTypeVersionService deviceTypeVersionService = new DeviceTypeVersionService((repositoryFactory));
        deviceTypeService.deviceTypeVersionService = deviceTypeVersionService;

        DeviceType deviceType = new DeviceType();
        deviceType.name = "name01";
        deviceTypeService.create(deviceType);

        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.deviceTypeId = deviceType.id;
        deviceTypeVersionService.create(deviceTypeVersion);

        try {
            deviceTypeService.delete(deviceType.id);
        } catch (NotFoundException e) {
            pass = true;
        } catch (Exception e) {
            pass = false;
        }

        Assert.assertEquals(pass, true);
    }
}
