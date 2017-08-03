package vn.ssdc.vnpt.subscriber.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.Device;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Huy Hieu on 11/25/2016.
 */

@Service
public class SubscriberDeviceService extends SsdcCrudService<Long, SubscriberDevice> {

    public static final String SUBSCRIBER_ID_COLUMN = "subscriber_id";
    public static final String DEVICE_ID_COLUMN = "device_id";

    @Autowired
    public SubscriberService subscriberService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    public SubscriberDeviceService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(SubscriberDevice.class);
    }

    public List<SubscriberDevice> findBySubscribeId(String subscriberId) {
        String whereExp = SUBSCRIBER_ID_COLUMN + "=?";
        List<SubscriberDevice> subscriberDevices = this.repository.search(whereExp, subscriberId);
        if (!ObjectUtils.empty(subscriberDevices)) {
            return subscriberDevices;
        }
        return new ArrayList<SubscriberDevice>();

    }


    public List<Subscriber> findByDeviceId(String deviceId) {
        String whereExp = DEVICE_ID_COLUMN + "=?";
        List<SubscriberDevice> subscriberDevices = this.repository.search(whereExp, deviceId);
        List<Subscriber> subscribers = new ArrayList<Subscriber>();
        if (!ObjectUtils.empty(subscriberDevices)) {
            for (SubscriberDevice subscriberDevice : subscriberDevices) {
                Subscriber subscriber = subscriberService.findBySubscribeId(subscriberDevice.subscriberId);
                if (subscriber != null) {
                    subscribers.add(subscriber);
                }
            }
        }
        return subscribers;
    }

    public Boolean replaceCPE(String oldDeviceId, String newDeviceId) {
        String whereExp = DEVICE_ID_COLUMN + "=?";
        List<SubscriberDevice> subscriberDevices = this.repository.search(whereExp, oldDeviceId);
        for (SubscriberDevice subscriberDevice : subscriberDevices) {
            String responseEntityBody = this.acsClient.getDevice(newDeviceId, "_deviceId").getBody();
            Set<String> parameters = new HashSet<String>();
            parameters.add("_deviceId._Manufacturer");
            parameters.add("_deviceId._OUI");
            parameters.add("_deviceId._ProductClass");
            parameters.add("_deviceId._SerialNumber");
            List<Device> devices = Device.fromJsonString(responseEntityBody, parameters);
            if (devices.size() > 0) {
                Device device = devices.get(0);
                subscriberDevice.deviceId = newDeviceId;
                subscriberDevice.manufacturer = device.manufacturer();
                subscriberDevice.oui = device.oui();
                subscriberDevice.productClass = device.productClass();
                subscriberDevice.serialNumber = device.serialNumber();
                this.update(subscriberDevice.id, subscriberDevice);
                acsClient.deleteDevice(oldDeviceId);
            }
        }
        return true;
    }

    public boolean findSubByDeviceId(String deviceId) {
        String whereExp = DEVICE_ID_COLUMN + "=?";
        int size = this.repository.search(whereExp, deviceId).size();
        if (size > 0) { return true; }
        return false;
    }
}
