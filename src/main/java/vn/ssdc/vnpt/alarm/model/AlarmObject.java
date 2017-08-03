package vn.ssdc.vnpt.alarm.model;

/**
 * Created by Admin on 6/6/2017.
 */
import io.searchbox.annotations.JestId;

public class AlarmObject {
    @JestId
    public String _id;

    public String eventTime;
    public String deviceId;
    public String sessionId;
    public String type;
    public String detail;
    public String tag;
    public String message;
}
