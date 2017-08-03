package vn.ssdc.vnpt.policy.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 3/13/2017.
 */
public class PolicyJob extends SsdcEntity<Long> {
    public String name;
    public String status;
    //INIT, EXCUTE, STOP
    public Long deviceGroupId;
    public List<String> externalDevices;
    public String externalFilename;
    public Long startAt;
    public Integer timeInterval;
    public Integer maxNumber;
    public List<String> events;
    public Boolean isImmediately;
    public String actionName;
    public Map<String, Object> parameters;
    public String presetId;
    public Integer limited;
    public Integer priority;

    private Long numberOfExecutions;
    private String deviceGroupName;

    public Long getNumberOfExecutions() {
        return numberOfExecutions;
    }

    public void setNumberOfExecutions(Long numberOfExecutions) {
        this.numberOfExecutions = numberOfExecutions;
    }

    public String getDeviceGroupName() {
        return deviceGroupName;
    }

    public void setDeviceGroupName(String deviceGroupName) {
        this.deviceGroupName = deviceGroupName;
    }
}
