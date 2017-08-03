package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 * Created by kiendt on 2/7/2017.
 */
public class Tr069Parameter extends SsdcEntity<Long> {
    public String path;
    public String dataType;
    public String defaultValue;
    public String rule;
    public String parentObject;
    public String version;
    public String description;
    public String access;
    public String otherAttributes;
    public String profileNames;



}
