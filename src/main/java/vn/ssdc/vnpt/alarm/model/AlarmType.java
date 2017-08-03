package vn.ssdc.vnpt.alarm.model;

import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.Map;
import java.util.Set;

/**
 * Created by thangnc on 23-May-17.
 */
public class AlarmType extends SsdcEntity<Long> {
    public String type;
    public String name;
    public Set<DeviceGroup> deviceGroups;
    public String severity;
    public Boolean notify;
    public Boolean monitor;
    public long aggregatedVolume;
    public String notifyAggregated;
    public Map<String,String> parameterValues;

    public Integer notification;
    public Integer timeSettings;
}
