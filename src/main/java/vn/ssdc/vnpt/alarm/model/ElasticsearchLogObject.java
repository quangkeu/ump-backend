package vn.ssdc.vnpt.alarm.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

/**
 * Created by Admin on 6/22/2017.
 */
public class ElasticsearchLogObject {
    @JestId
    public String _id;
    @SerializedName("@timestamp")
    public String timestamp;
    public String message;
}
