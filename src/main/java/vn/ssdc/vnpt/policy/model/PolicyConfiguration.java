package vn.ssdc.vnpt.policy.model;

import java.io.Serializable;

/**
 * Created by THANHLX on 4/12/2017.
 */
public class PolicyConfiguration implements Serializable{
    public String type;
    public String name;
    public String value;
    public Long policyJobId;
    public String fileId;
    public String fileType;
    public String url;
}
