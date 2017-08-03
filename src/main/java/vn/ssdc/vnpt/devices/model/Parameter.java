package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.utils.ObjectUtils;

/**
 * Created by vietnq on 11/2/16.
 */
public class Parameter {
    public String path;
    public String subPath;
    public String shortName;
    public String dataType;
    public String value;
    public String defaultValue;
    //range [1-4], list of possible values [1,2,3]
    public String rule;
    public String inputType;
    public Integer useSubscriberData;
    public String tr069Name;
    public String access; //ReadOnly or ReadWrite
    public String parentObject;
    public String subscriberData;
    public Boolean instance;
    public String tr069ParentObject;

    public String getValue() {
        return ObjectUtils.empty(value) ? (ObjectUtils.empty(defaultValue) ? "" : defaultValue) : value;
    }
}
