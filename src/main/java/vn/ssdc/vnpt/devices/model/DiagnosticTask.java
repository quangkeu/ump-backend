package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.Map;

/**
 * Created by Admin on 2/17/2017.
 */
public class DiagnosticTask extends SsdcEntity<Long> {
    public String deviceId;
    public String diagnosticsName;
    public Map<String, Parameter> parameterFull;
    public Map<String, Object> request;
    public Map<String, String> result;
    public Integer status;//1: SUCCESS - 2: FAIL
    public Long completed;
    public String taskId;
}
