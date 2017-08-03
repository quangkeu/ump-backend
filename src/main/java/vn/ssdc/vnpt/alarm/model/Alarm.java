package vn.ssdc.vnpt.alarm.model;

import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.vnpt.ssdc.jdbc.SsdcEntity;
import vn.vnpt.ssdc.jdbc.annotations.ExtendField;

import java.util.Set;

/**
 * Created by Lamborgini on 5/24/2017.
 */

public class Alarm  extends SsdcEntity<Long> {

    public String deviceId;
    public long alarmTypeId;
    public String alarmTypeName;
    public String alarmName;
    public long raised;
    public String status;
    public String description;
    public String severity;
    public Set<DeviceGroup> deviceGroups;
    @ExtendField
    public Long numberOfRows;

}
