package vn.ssdc.vnpt.logging.model;

import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

public class LoggingPolicy extends SsdcEntity<Long> {
    public String deviceId;
    public String taskId;
    public Long policyJobId;
    public Integer status;
    public Long completed;
    public String errorCode;
    public String errorText;

    public PolicyTask toPolicyTask() {
        PolicyTask policyTask = new PolicyTask();
        policyTask.deviceId = deviceId;
        policyTask.policyJobId = policyJobId;
        policyTask.taskId = taskId;
        policyTask.created = created;
        policyTask.completed = completed;
        policyTask.status = status;
        policyTask.errorCode = errorCode;
        policyTask.errorText = errorText;

        return  policyTask;
    }
}