package vn.ssdc.vnpt.logging.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

public class ElkLoggingDevice {
    @JestId
    public String _id;
    public String message;
    @SerializedName("@timestamp")
    public String timestamp;

}