package vn.ssdc.vnpt.alarm.services;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ParseException;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.alarm.model.AlarmTypeQuartzJob;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.*;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by thangnc on 23-May-17.
 */
@Service
public class AlarmTypeService extends SsdcCrudService<Long, AlarmType> {
    private static final Logger logger = LoggerFactory.getLogger(AlarmType.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    public AlarmTypeService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(AlarmType.class);
    }

    public void createQuartzJob(Long alarmTypeId, Integer intTimeInterval) throws ParseException, SchedulerException {
        JobDetail job = JobBuilder.newJob(AlarmTypeQuartzJob.class).withIdentity("AlarmType_" + alarmTypeId).build();
        job.getJobDataMap().put("alarmTypeJobId", alarmTypeId);
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_AlarmType_" + alarmTypeId)
                .withSchedule(simpleSchedule().withIntervalInMinutes(intTimeInterval).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public void deleteQuartzJob(Long alarmTypeId) throws SchedulerException {
        String strJob = "AlarmType_".concat(Long.toString(alarmTypeId));
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);
    }

    public void deleteTriger(Long alarmTypeId) throws SchedulerException {
        String strTrigger = "Trigger_AlarmType_".concat(Long.toString(alarmTypeId));
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }

    public List<AlarmType> findByNotify(){
        String whereExp = " notify = 1 ";
        List<AlarmType> lstAlarmType = this.repository.search(whereExp);
        return lstAlarmType;
    }

    public List<AlarmType> findByMonitoring(){
        String whereExp = " monitor = 1 ";
        List<AlarmType> lstAlarmType = this.repository.search(whereExp);
        return lstAlarmType;
    }

    public List<AlarmType> findByPage(int offset, int limit) {
        String whereExp = "1=1 order by id desc limit ?,? ";
        List<AlarmType> deviceGroups = this.repository.search(whereExp, (offset - 1) * limit, limit);
        return deviceGroups;
    }

    public List<AlarmType> findByName(String name) {
        String whereExp = "name=?";
        List<AlarmType> deviceGroups = this.repository.search(whereExp, name);
        return deviceGroups;
    }

    public List<AlarmType> search(String type, String name, String severity, String group, String prefix) {
        String whereExp = "1=1";
        if (!("").equals(prefix)) {
            whereExp = " type like '%"  + prefix + "%' and name like '%" + prefix + "%' and severity like '%" + prefix + "%' and device_groups like '%" + prefix + "%'";
        } else {
            if (!("").equals(type)) whereExp = whereExp + " and type like '%" + type + "%'";
            if (!("").equals(name)) whereExp = whereExp + " and name like '%" + name + "%'";
            if (!("").equals(severity)) whereExp = whereExp + " and severity like '%" + severity + "%'";
            if (!("").equals(group)) whereExp = whereExp + " and device_groups like '%\"id\":"+group+",%'";
        }
        List<AlarmType> deviceGroups = this.repository.search(whereExp);
        return deviceGroups;
    }

    public List<AlarmType> getListAlarmByDeviceGroupId(String deviceGroupId) {
        return this.repository.search("device_groups LIKE '%\"id\":"+deviceGroupId+"%'");
    }

    public void refreshParameter(Long alarmTypeId) {

        AlarmType alarmType = get(alarmTypeId);

        Map<String,String> parameterValues = alarmType.parameterValues;
        List<String> parameterList = new LinkedList<>();

        for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
            parameterList.add(entry.getKey());
        }

        Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
        Iterator<DeviceGroup> deviceGroupIterator =  deviceGroups.iterator();
        while (deviceGroupIterator.hasNext()) {
            DeviceGroup deviceGroup = deviceGroupIterator.next();
            List<String> listDevices = deviceGroupService.getListDeviceByGroup(deviceGroup.id);
            for(int i = 0; i < listDevices.size(); i++) {
                acsClient.getParameterValues(listDevices.get(i), parameterList, true);
            }
        }

    }

}
