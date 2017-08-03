package vn.ssdc.vnpt.subscriber.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 * Created by Huy Hieu on 11/25/2016.
 */
public class SubscriberDevice extends SsdcEntity<Long> {

    public String subscriberId;
    public String deviceId;
    public String manufacturer;
    public String oui;
    public String productClass;
    public String serialNumber;

    public void generateDeviceId() {

        // Create device id from oui, productClass, serialNumber.
        this.deviceId = oui + "-" + productClass + "-" + serialNumber;
//        this.deviceId = oui + productClass + "-" + serialNumber;
    }
}
