package vn.ssdc.vnpt.logging.model;

import java.util.LinkedList;
import java.util.List;

public class LoggingUserAction {
    public String action;
    public String time;
    public String affected;
    public String taskId;
    public List<LoggingDevice> loggingDevices;

    public LoggingUserAction() {
        this.loggingDevices = new LinkedList<LoggingDevice>();
    }

}