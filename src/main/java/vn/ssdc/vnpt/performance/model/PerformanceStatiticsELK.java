package vn.ssdc.vnpt.performance.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

/**
 * Created by thangnc on 06-Jul-17.
 */
public class PerformanceStatiticsELK {

    public Long performanceSettingId;
    public String stasticsType;
    public String type;
    public String parameterNames;
    public String valueChanges;

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;

}
