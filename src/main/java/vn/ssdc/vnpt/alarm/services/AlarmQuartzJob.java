package vn.ssdc.vnpt.alarm.services;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.devices.model.DateUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Admin on 6/28/2017.
 */
public class AlarmQuartzJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(AlarmQuartzJob.class);

    @Autowired
    public AlarmService alarmService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            if(jobExecutionContext.getPreviousFireTime() != null) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String fromDateTime = df.format(jobExecutionContext.getPreviousFireTime());
                String endDateTime = df.format(jobExecutionContext.getScheduledFireTime());
                alarmService.processAlarm(fromDateTime, endDateTime);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
