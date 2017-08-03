package vn.ssdc.vnpt.alarm.model;



import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by thangnc on 10-Jul-17.
 */
public class AlarmPreset  implements Serializable {

    public String _id;
    public String channel;
    public Integer weight;
    public String precondition;
    public List<AlarmConfiguration> configurations;
    public String schedule;
    public Map<String,Boolean> events;

}
