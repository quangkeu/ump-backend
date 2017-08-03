package vn.ssdc.vnpt.test.policy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.policy.endpoints.PolicyJobEnpoint;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.policy.model.PolicyQuartzJob;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.SsdcDao;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by Admin on 4/19/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class PolicyJobServiceTest {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void createQuartzJob() throws Exception {
        /*
        PolicyQuartzServer.start();

        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);
        PolicyJobEnpoint policyJobEnpoint = new PolicyJobEnpoint(policyJobService);

        String string_date = "30-December-2017";

        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
        long startDate = 0;
        try {
            Date d = f.parse(string_date);
            startDate = d.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long policyJobId = 1;
        int timeInterval = 30;
        Properties prop = policyJobEnpoint.getQuartzProb();

        policyJobService.createQuartzJob(startDate, policyJobId, timeInterval, prop);

        String strTrigger = "Trigger_".concat(Long.toString(policyJobId));
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        Scheduler scheduler = new StdSchedulerFactory(prop).getScheduler();
        scheduler.getTrigger(triggerKey);
        Assert.assertNotNull(scheduler.getTrigger(triggerKey));

        //delete trigger
        policyJobService.deleteTriger(policyJobId);
        policyJobService.deleteQuartzJob(policyJobId);
        */
    }

    @Test
    public void deleteTriger() throws Exception {
        /*
        PolicyQuartzServer.start();

        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);
        PolicyJobEnpoint policyJobEnpoint = new PolicyJobEnpoint(policyJobService);
        Properties prop = policyJobEnpoint.getQuartzProb();

        JobDetail job = JobBuilder.newJob(PolicyQuartzJob.class).withIdentity("Job_" + 1).build();
        job.getJobDataMap().put("policyJobId", 1);

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + 1)
                .startNow()
                .withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever())
                .build();

        Scheduler scheduler = new StdSchedulerFactory(prop).getScheduler();
        scheduler.scheduleJob(job, trigger);

        policyJobService.deleteTriger(1l);

        TriggerKey triggerKey = new TriggerKey("Trigger_1");
        Assert.assertNull(scheduler.getTrigger(triggerKey));

        policyJobService.deleteQuartzJob(1l);
        */
    }

    @Test
    public void deleteQuartzJob() throws Exception {
        /*
        PolicyQuartzServer.start();

        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);
        PolicyJobEnpoint policyJobEnpoint = new PolicyJobEnpoint(policyJobService);
        Properties prop = policyJobEnpoint.getQuartzProb();

        JobDetail job = JobBuilder.newJob(PolicyQuartzJob.class).withIdentity("Job_" + 1).build();
        job.getJobDataMap().put("policyJobId", 1);

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_" + 1)
                .startNow()
                .withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever())
                .build();

        Scheduler scheduler = new StdSchedulerFactory(prop).getScheduler();
        scheduler.scheduleJob(job, trigger);

        policyJobService.deleteQuartzJob(1l);

        JobKey jobKey = new JobKey("Job_1");
        Assert.assertNull(scheduler.getJobDetail(jobKey));

        policyJobService.deleteTriger(1l);
        */
    }

    @Test
    public void createUpdatePreset() throws Exception {

    }

    @Test
    public void deletePreset() throws Exception {

    }

    @Test
    public void beforeCreate() throws Exception {
        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);
        PolicyJob policyJob =  new PolicyJob();
        policyJob.status = "test";
        policyJobService.beforeCreate(policyJob);
        Assert.assertEquals("INIT",policyJob.status);
    }

    @Test
    public void findJobExecute() throws Exception {
        /*
        PolicyQuartzServer.start();
        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);

        PolicyJob policyJob = new PolicyJob();
        policyJob.id = 1l;
        policyJob.deviceGroupId = 1l;
        policyJob.status = "1";

        policyJobService.repository.save(policyJob);

        Assert.assertTrue(policyJobService.findJobExecute("1","1"));
        */
    }

    @Test
    public void getPage() throws Exception {
        /*
        PolicyQuartzServer.start();
        PolicyJobService policyJobService = new PolicyJobService(repositoryFactory);

        PolicyJob policyJob = new PolicyJob();
        policyJob.id = 1l;
        policyJob.deviceGroupId = 1l;
        policyJob.status = "1";

        policyJobService.repository.save(policyJob);

        Assert.assertNotNull(policyJobService.getPage(1,1));
        */
    }

}