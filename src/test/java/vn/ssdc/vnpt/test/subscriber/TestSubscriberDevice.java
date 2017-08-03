package vn.ssdc.vnpt.test.subscriber;

import org.junit.Assert;
import org.junit.Test;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;

/**
 * Created by vietnq on 12/22/16.
 */
public class TestSubscriberDevice {
    @Test
    public void testGenerateDeviceId() {
        SubscriberDevice subscriberDevice = new SubscriberDevice();
        //202BC1-BM632w-0000000
        subscriberDevice.oui = "202BC1";
        subscriberDevice.productClass = "BM632w";
        subscriberDevice.serialNumber = "0000000";
        subscriberDevice.generateDeviceId();
        Assert.assertEquals(subscriberDevice.deviceId,"202BC1-BM632w-0000000");
    }
}
