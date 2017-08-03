package vn.ssdc.vnpt;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.alarm.services.AlarmDetailQuartzJob;
import vn.ssdc.vnpt.alarm.services.AlarmQuartzJob;
import vn.ssdc.vnpt.alarm.services.MonitoringQuartzJob;
import vn.ssdc.vnpt.logging.services.LoggingUserQuartzJob;

import java.util.Date;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by THANHLX on 6/29/2017.
 */
@Component
public class QuartzJobLoader implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(QuartzJobLoader.class);

    @Autowired
    private Scheduler scheduler;

    public void run(ApplicationArguments args) {
        initLoggingUserQuartz();
        initAlarmDetailQuartz();
        initAlarmQuartz();
        initMonitoringQuartz();
    }

    public void initLoggingUserQuartz(){
        try {
            JobKey jobKey = new JobKey("Logging User Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist logging user quartz job");
            } else {
                Date startDate = new Date();
                JobDetail job = JobBuilder.newJob(LoggingUserQuartzJob.class).withIdentity("Logging User Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Logging User Trigger")
                        .startAt(startDate)
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initAlarmDetailQuartz(){
        try {
            JobKey jobKey = new JobKey("Alarm Detail Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist alarm detail quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(AlarmDetailQuartzJob.class).withIdentity("Alarm Detail Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Alarm Detail Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initAlarmQuartz(){
        try {
            JobKey jobKey = new JobKey("Alarm Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist alarm quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(AlarmQuartzJob.class).withIdentity("Alarm Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Alarm Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void initMonitoringQuartz(){
        try {
            JobKey jobKey = new JobKey("Monitoring Job");
            if (scheduler.getJobDetail(jobKey) != null) {
                logger.info("Exist monitoring quartz job");
            } else {
                JobDetail job = JobBuilder.newJob(MonitoringQuartzJob.class).withIdentity("Monitoring Job").build();
                Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Monitoring Trigger")
                        .startNow()
                        .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
