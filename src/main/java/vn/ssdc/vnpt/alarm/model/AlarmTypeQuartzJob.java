package vn.ssdc.vnpt.alarm.model;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.services.AlarmTypeService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by thangnc on 27-Jul-17.
 */
public class AlarmTypeQuartzJob implements Job {

    public static final Logger logger = LoggerFactory.getLogger(AlarmTypeQuartzJob.class);

    @Autowired
    private AlarmTypeService alarmTypeService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        alarmTypeService.refreshParameter(jdm.getLong("alarmTypeJobId"));
    }

}
