package vn.ssdc.vnpt.alarm.endpoints;

import io.swagger.annotations.Api;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.services.AlarmDetailsService;
import vn.ssdc.vnpt.alarm.services.AlarmService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Lamborgini on 5/24/2017.
 */
@Component
@Path("alarm")
@Api("Alarm")
@Produces(APPLICATION_JSON)
public class AlarmEndPoint extends SsdcCrudEndpoint<Long, Alarm> {
    private AlarmService alarmService;
    private AlarmDetailsService alarmDetailsService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    public AlarmEndPoint(AlarmService alarmService) {
        this.service = this.alarmService = alarmService;

    }

    @GET
    @Path("delete-quartz-job")
    public void deleteQuartzJob() throws SchedulerException{
        String strJob = "Logging User Job";
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        String strTrigger = "Logging User Trigger";
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Alarm Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Alarm Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Alarm Detail Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Alarm Detail Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Monitoring Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Monitoring Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }

    @GET
    @Path("/processing-value-change")
    public boolean processingValueChange(@DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                         @DefaultValue("") @QueryParam("toDateTime") String toDateTime) throws IOException, ParseException, SchedulerException {
        this.alarmService.monitoringCWMPLog(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/processing-alarm-detail")
    public boolean processingAlarmDetail(@DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                         @DefaultValue("") @QueryParam("toDateTime") String toDateTime) throws IOException, ParseException, SchedulerException {
        this.alarmService.processingAlarmDetail(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/processing-alarm")
    public boolean processingAlarm(@DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                         @DefaultValue("") @QueryParam("toDateTime") String toDateTime) throws IOException, ParseException, SchedulerException {
        this.alarmService.processAlarm(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/search-alarm")
    public List<Alarm> searchAlarm(@DefaultValue("20") @QueryParam("limit") String limit,
                                   @DefaultValue("0") @QueryParam("indexPage") String indexPage,
                                   @DefaultValue("0") @QueryParam("whereExp") String whereExp) {
        return this.alarmService.searchAlarm(limit, indexPage, whereExp);
    }

    @GET
    @Path("/count-alarm")
    public int countAlarmType(@DefaultValue("") @QueryParam("whereExp") String whereExp) {
        return this.alarmService.countAlarm(whereExp);
    }

    @GET
    @Path("/get-alarm-name-by-alarm-type")
    public List<Alarm> getAlarmNameByAlarmType(@DefaultValue("") @QueryParam("alarmType") String alarmType) {
        return this.alarmService.getAlarmNameByAlarmType(alarmType);
    }

    @GET
    @Path("/view-graph-severity-alarm")
    public List<Alarm> viewGraphSeverityAlarm(@DefaultValue("") @QueryParam("whereExp") String whereExp) {
        return this.alarmService.viewGraphSeverityAlarm(whereExp);
    }

    @GET
    @Path("/view-graph-number-of-alarm-type")
    public List<Alarm> viewGraphNumberOfAlarmType(@DefaultValue("") @QueryParam("whereExp") String whereExp) {
        return this.alarmService.viewGraphNumberOfAlarmType(whereExp);
    }

}
