package vn.ssdc.vnpt.logging.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class LoggingDeviceActivity extends SsdcEntity<String> {
    public String taskId;
    public String taskName;
    public String parameter;
    public String createdTime;
    public String completedTime;
    public String errorCode;
    public String errorText;

}