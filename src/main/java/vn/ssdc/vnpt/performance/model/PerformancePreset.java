package vn.ssdc.vnpt.performance.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by thangnc on 13-Jul-17.
 */
public class PerformancePreset implements Serializable {

    public String _id;
    public String channel;
    public Integer weight;
    public String precondition;
    public List<PerformanceConfiguration> configurations;
    public String schedule;
    public Map<String,Boolean> events;

}
