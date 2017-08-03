package vn.ssdc.vnpt.performance.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

/**
 * Created by thangnc on 27-Jun-17.
 */
public class PerformanceELK {

    @JestId
    public String _id;
    public  String deviceId;
    public String parameterName;
    public String value;

    @SerializedName("@timestamp")
    public String timestamp;

}
