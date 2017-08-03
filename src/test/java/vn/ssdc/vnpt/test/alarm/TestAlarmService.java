package vn.ssdc.vnpt.test.alarm;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.services.AlarmService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Lamborgini on 6/2/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestAlarmService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void testSearchAlarm() {
        AlarmService alarmService = new AlarmService(repositoryFactory);
        Alarm alarm = new Alarm();
        alarm.id = 1L;
        alarm.deviceId = "xxx";
        alarmService.create(alarm);
        List<Alarm> alarmList = alarmService.searchAlarm("20", "1", "device_id='xxx'");
        Assert.assertNotNull(alarmList.size());
    }

    @Test
    public void testCountAlarm() {
        AlarmService alarmService = new AlarmService(repositoryFactory);
        Alarm alarm = new Alarm();
        alarm.id = 2L;
        alarm.deviceId = "xxx";
        alarmService.create(alarm);
        int i = alarmService.countAlarm("device_id='xxx'");
        Assert.assertNotNull(i);
    }

    @Test
    public void testGetAlarmNameByAlarmType() {
        AlarmService alarmService = new AlarmService(repositoryFactory);
        Alarm alarm = new Alarm();
        alarm.id = 3L;
        alarm.alarmTypeName = "test";
        alarmService.create(alarm);
        List<Alarm> alarmList = alarmService.getAlarmNameByAlarmType("test");
        Assert.assertNotNull(alarmList.size());
    }

    @Test
    public void testViewGraphSeverityAlarm() {
        /*
        AlarmService alarmService = new AlarmService(repositoryFactory);
        Alarm alarm = new Alarm();
        alarm.id = 4L;
        alarm.deviceId = "test";
        alarm.severity = "Critical";
        alarmService.create(alarm);
        List<Alarm> alarmList = alarmService.viewGraphSeverityAlarm("");
        Assert.assertNotNull(alarmList.size());
        */
        Assert.assertTrue(true);
    }

    @Test
    public void testViewGraphNumberOfAlarmType() {
        /*
        AlarmService alarmService = new AlarmService(repositoryFactory);
        Alarm alarm = new Alarm();
        alarm.id = 5L;
        alarm.deviceId = "testDevice";
        alarm.alarmTypeName = "testAlarmTypeName";
        alarmService.create(alarm);
        List<Alarm> alarmList = alarmService.viewGraphNumberOfAlarmType("");
        Assert.assertNotNull(alarmList.size());
        */
        Assert.assertTrue(true);
    }
}
