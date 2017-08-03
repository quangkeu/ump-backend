package vn.ssdc.vnpt.logging.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoggingDevice{
    public String session;
    public String deviceId;
    public String time;
    public String status;
    public Map<Integer, CwmpLoggingDevice> cwmps;

    public LoggingDevice() {
        this.cwmps = new LinkedHashMap<Integer, CwmpLoggingDevice>();
    }

}