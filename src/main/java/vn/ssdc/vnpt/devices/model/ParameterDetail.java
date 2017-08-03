package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kiendt on 2/6/2017.
 */
public class ParameterDetail extends SsdcEntity<Long> {

    // public field will map with columns in database
    public String path;
    public String shortName;
    public String dataType;
    public String defaultValue;
    public String rule;

    public String access;
    public String parentObject;
    public String version;
    public String description;
    public Long deviceTypeVersionId;
    public String tr069Name;
    public Boolean instance;
    public String tr069ParentObject;
    // public Set<Long> profile;
    // Use set string temporarily to workaround json convert number to double
    public Set<String> profile;

    private String value;

    public ParameterDetail() {
        profile = new HashSet<String>();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
