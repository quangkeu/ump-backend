package vn.ssdc.vnpt.subscriber.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Subscriber extends SsdcEntity<Long> {

    public String subscriberId;
    public Set<String> subscriberDataTemplateIds;
    public Map<String,String> subscriberData;
    private Set<String> deviceIds;

    @JsonProperty
    public void setDeviceIds(Set<String> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public Set<String> getDeviceIds() {
        return deviceIds;
    }

    public Subscriber() {
        subscriberData = new LinkedHashMap<String, String>();
        subscriberDataTemplateIds = new LinkedHashSet<String>();
        deviceIds = new LinkedHashSet<String>();
    }

}
