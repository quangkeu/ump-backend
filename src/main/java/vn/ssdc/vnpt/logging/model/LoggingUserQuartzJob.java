package vn.ssdc.vnpt.logging.model;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.logging.services.LoggingUserService;
import vn.ssdc.vnpt.policy.services.PolicyJobService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by THANHLX on 6/29/2017.
 */
public class LoggingUserQuartzJob implements Job {
    @Autowired
    LoggingUserService loggingUserService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fromDateTime = df.format(jobExecutionContext.getPreviousFireTime());
        String endDateTime = df.format(new Date());
        loggingUserService.getUpdateMysql(fromDateTime, endDateTime);
    }
}
