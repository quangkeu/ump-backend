package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;
import vn.vnpt.ssdc.jdbc.annotations.Serialized;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vietnq on 11/11/16.
 */
public class DeviceTypeVersion extends SsdcEntity<Long> {
    public Long deviceTypeId;
    public String firmwareVersion;
    public Map<String, Parameter> parameters;
    public Map<String, Tag> diagnostics;
    public String modelName;
    public String manufacturer;
    public String oui;
    public String productClass;
    public DeviceTypeVersion() {
        parameters = new HashMap<String, Parameter>();
        diagnostics = new HashMap<String, Tag>();
    }
}
