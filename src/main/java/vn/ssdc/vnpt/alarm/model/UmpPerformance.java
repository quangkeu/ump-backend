package vn.ssdc.vnpt.alarm.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

/**
 * Created by Admin on 6/29/2017.
 */
public class UmpPerformance {
    @JestId
    public String _id;
    @SerializedName("@timestamp")
    public String timestamp;
    public String deviceId;
    public String parameterName;
    public String value;
    public String tags;

}
