package vn.ssdc.vnpt.logging.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class CwmpLoggingDevice extends SsdcEntity<Long> {

    public String type;
    public String actor;
    public String affected;
    public String cwmp;
    public String eventCode;
    public String time;
    public String message;

    private static final String ACTOR_ACS = "ACS";

    public void setTypeRequest(String deviceId) {
        this.type = "REQUEST";
        this.actor = deviceId;
        this.affected = ACTOR_ACS;
    }

    public void setTypeResponse(String deviceId) {
        this.type = "RESPONSE";
        this.actor = ACTOR_ACS;
        this.affected = deviceId;
    }

}
