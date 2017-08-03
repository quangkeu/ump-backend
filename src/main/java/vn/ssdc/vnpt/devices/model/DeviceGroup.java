package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.Map;

/**
 * Created by thangnc on 06-Feb-17.
 */
public class DeviceGroup extends SsdcEntity<Long> {
    public String name;
    public Map<String, Filter> filters;
    public String query;
    public String manufacturer;
    public String modelName;
    public String firmwareVersion;
    public String label;
    public String oui;
    public String productClass;
}
