package vn.ssdc.vnpt.policy.model;

/**
 * Created by Admin on 2/27/2017.
 */

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import vn.ssdc.vnpt.policy.services.PolicyJobService;

import javax.inject.Inject;
import javax.inject.Named;

public class PolicyQuartzJob implements Job {

    @Autowired
    PolicyJobService policyJobService;

    // Excute a job policy
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        Long policyJobId = jdm.getLong("policyJobId");
        policyJobService.createUpdatePreset(policyJobId);
    }
}
