package vn.ssdc.vnpt.policy.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;
import vn.vnpt.ssdc.jdbc.annotations.ExtendField;
import vn.vnpt.ssdc.jdbc.annotations.Serialized;

/**
 * Created by Admin on 3/13/2017.
 */
public class PolicyTask extends SsdcEntity<Long> {
    public String deviceId;
    public Long policyJobId;
    public Integer status;//1: SUCCESS - 2: FAIL
    public Long completed;
    public String taskId;
    public String errorCode;
    public String errorText;
    @ExtendField
    public Long numberOfRows;
}
