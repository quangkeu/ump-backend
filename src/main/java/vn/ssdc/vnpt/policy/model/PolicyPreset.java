package vn.ssdc.vnpt.policy.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by Huy Hieu on 12/9/2016.
 */
public class PolicyPreset implements Serializable{
    public String _id;
    public String precondition;
    public List<PolicyConfiguration> configurations;
    public Map<String,Boolean> events;
    public int maxNumber;
    public int currentNumber;
}