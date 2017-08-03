package vn.ssdc.vnpt.performance.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.List;

/**
 * Created by thangnc on 21-Jun-17.
 */
public class PerformanceSetting extends SsdcEntity<Long> {

    public String stasticsType;
    public String type;
    public Integer stasticsInterval;
    public Integer monitoring;
    public String deviceId;
    public Long deviceGroupId;
    public List<String> externalDevices;
    public String external_filename;
    public Long start;
    public Long end;

    public String manufacturer;
    public String modelName;
    public String serialNumber;
    public List<String> parameterNames;

}
