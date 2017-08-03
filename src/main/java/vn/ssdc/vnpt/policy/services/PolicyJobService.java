package vn.ssdc.vnpt.policy.services;


import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.policy.model.*;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.text.ParseException;
import java.util.*;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by Admin on 3/13/2017.
 */
@Service
public class PolicyJobService extends SsdcCrudService<Long, PolicyJob> {
    private static final Logger logger = LoggerFactory.getLogger(PolicyJobService.class);

    @Autowired
    AcsClient acsClient;

    @Autowired
    PolicyTaskService policyTaskService;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    public PolicyJobService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PolicyJob.class);
    }

    //Create a new Quartz Job for Policy
    public void createQuartzJob(Long strStartDate, Long policyJobId, Integer intTimeInterval) throws ParseException, SchedulerException {
        Date dStartDate = new Date(strStartDate);
        JobDetail job = JobBuilder.newJob(PolicyQuartzJob.class).withIdentity("Job_" + policyJobId).build();
        job.getJobDataMap().put("policyJobId", policyJobId);
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + policyJobId)
                .startAt(dStartDate)
                .withSchedule(simpleSchedule().withIntervalInSeconds(60*intTimeInterval).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    //Unscheduling a Particular Trigger of Job
    public void deleteTriger(Long policyJobsId) throws SchedulerException{
        String strTrigger = "Trigger_".concat(Long.toString(policyJobsId));
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }

    //Deleting a Job and Unscheduling All of Its Triggers
    public void deleteQuartzJob(Long policyJobsId) throws SchedulerException{
        String strJob = "Job_".concat(Long.toString(policyJobsId));
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);
    }

    public void createTask(String deviceId, PolicyJob policyJob){
        if ("parameters".equals(policyJob.actionName)) {
            Map<String, Object> parameters = policyJob.parameters;
            Map<String, Object> parameterValues = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                LinkedTreeMap parameter = LinkedTreeMap.class.cast(entry.getValue());
                parameterValues.put(entry.getKey(), parameter.get("value"));
            }
            acsClient.setParameterValues(deviceId, parameterValues, true, policyJob.id);
        } else if ("backup".equals(policyJob.actionName)) {
            acsClient.uploadFile(deviceId, "1 Vendor Configuration File", true, policyJob.id);
        } else if ("restore".equals(policyJob.actionName)) {
            BackupFile backupFile = acsClient.searchBackupFile(deviceId);
            if(backupFile != null) {
                acsClient.downloadFile(deviceId, backupFile.id, backupFile.filename, true, policyJob.id);
            }
        } else if ("reboot".equals(policyJob.actionName)) {
            acsClient.reboot(deviceId, true, policyJob.id);
        } else if ("factoryReset".equals(policyJob.actionName)) {
            acsClient.factoryReset(deviceId, true, policyJob.id);
        } else if ("updateFirmware".equals(policyJob.actionName)) {
            String strFileId = policyJob.parameters.get("fileId").toString();
            acsClient.downloadFile(deviceId, strFileId, "", true, policyJob.id);
        }
    }

    public void createUpdatePreset(Long policyJobId) {
        logger.info("Run update policy preset "+policyJobId);
        PolicyJob policyJob = this.repository.findOne(policyJobId);
        PolicyPreset policyPreset = new PolicyPreset();
        if (policyJob.deviceGroupId == null) {
            List<String> listDeviceIds = new ArrayList<String>();
            for(String deviceId : policyJob.externalDevices){
                listDeviceIds.add(String.format("{\"_id\":\"%s\"}", deviceId));
            }
            policyPreset.precondition = String.format("{\"$or\":[%s]}",StringUtils.join(listDeviceIds,","));
        }
        else{
            DeviceGroup deviceGroup = deviceGroupService.get(policyJob.deviceGroupId);
            policyPreset.precondition = deviceGroup.query;
        }
        Map<String, Boolean> events = new HashMap<String, Boolean>();
        for(String event : policyJob.events) {
            events.put(event,true);
        }
        policyPreset.events = events;
        if ("parameters".equals(policyJob.actionName)) {
            Map<String, Object> parameters = policyJob.parameters;
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                PolicyConfiguration policyConfiguration = new PolicyConfiguration();
                policyConfiguration.policyJobId = policyJobId;
                policyConfiguration.type = "value";
                policyConfiguration.name = entry.getKey();
                LinkedTreeMap parameter = LinkedTreeMap.class.cast(entry.getValue());
                policyConfiguration.value = parameter.get("value").toString();
                configurations.add(policyConfiguration);
            }
            policyPreset.configurations = configurations;
        }
        else if("reboot".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJobId;
            policyConfiguration.type = "reboot";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if("factoryReset".equals(policyJob.actionName)) {
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJobId;
            policyConfiguration.type = "reboot";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if("updateFirmware".equals(policyJob.actionName) || "downloadVendorConfigurationFile".equals(policyJob.actionName)){
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJobId;
            policyConfiguration.type = "download";
            policyConfiguration.fileId = policyJob.parameters.get("fileId").toString();
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if("restore".equals(policyJob.actionName)){
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJobId;
            policyConfiguration.type = "restore";
            policyConfiguration.fileId = "";
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        else if("backup".equals(policyJob.actionName)){
            List<PolicyConfiguration> configurations = new ArrayList<PolicyConfiguration>();
            PolicyConfiguration policyConfiguration = new PolicyConfiguration();
            policyConfiguration.policyJobId = policyJobId;
            policyConfiguration.type = "backup";
            //policyConfiguration.fileType = policyJob.parameters.get("fileType").toString();
            policyConfiguration.fileType = "1 Vendor Configuration File";
            policyConfiguration.url = String.format("%s/backup-files/%s", acsClient.getBackupFileEndpoint(), policyConfiguration.fileType.substring(0, 1));
            configurations.add(policyConfiguration);
            policyPreset.configurations = configurations;
        }
        if(!policyJob.isImmediately && policyJob.maxNumber != null){
            policyPreset.maxNumber = policyJob.maxNumber;
        }
        policyPreset.currentNumber = 0;
        if(policyJob.presetId != null) {
            policyPreset._id = "Policy Job : " + policyJobId;
        }
        acsClient.createPolicyPreset(policyPreset, "Policy Job " + policyJobId);
        if(policyJob.presetId == null){
            policyJob.presetId = "Policy Job : " + policyJobId;
            update(policyJobId, policyJob);
        }
    }

    public void deletePreset(Long policyJobId) {
        acsClient.deletePolicyPreset("Policy Job " + policyJobId);
    }

    @Override
    public void beforeCreate(PolicyJob policyJob) {
        policyJob.status = "INIT";
    }

    @Override
    public void afterDelete(PolicyJob policyJob){
        try {
            //1st.Delete Job
            deleteQuartzJob(policyJob.id);
            //2st.Delete Trigger
            deleteTriger(policyJob.id);
            //3st.Delete Preset
            deletePreset(policyJob.id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    public Boolean findJobExecute(String device_group_id, String status) {
        List<PolicyJob> policyJobsList = null;
        Boolean exitJobs = false;
        String whereExp = "device_group_id=? and status=?";
        if(("").equals(status)) {
            whereExp = "device_group_id=?";
            policyJobsList = this.repository.search(whereExp, Long.parseLong(device_group_id));
        }
        else policyJobsList = this.repository.search(whereExp, Long.parseLong(device_group_id), status);
        if(policyJobsList.size() > 0) exitJobs = true;
        return exitJobs;
    }

    public Page<PolicyJob> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public Page<PolicyJob> getPageWithNumberOfExecution(int page, int limit) {
        Page<PolicyJob> policyJobPage = this.repository.findAll(new PageRequest(page, limit));

        for (PolicyJob policyJob : policyJobPage.getContent()) {
            policyJob.setNumberOfExecutions(policyTaskService.count(String.format("policy_job_id=%s", policyJob.id)));

            if(!ObjectUtils.empty(policyJob.deviceGroupId)) {
                policyJob.setDeviceGroupName(deviceGroupService.get(policyJob.deviceGroupId).name);
            }
        }

        return policyJobPage;
    }
}
