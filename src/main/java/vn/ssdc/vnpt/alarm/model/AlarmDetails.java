package vn.ssdc.vnpt.alarm.model;

import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.Set;

/**
 * Created by Admin on 6/8/2017.
 */
public class AlarmDetails extends SsdcEntity<Long> {
    public long alarm_type_id;
    public String alarm_type;
    public String alarm_type_name;
    public String device_id;
    public Set<DeviceGroup> deviceGroups;
}