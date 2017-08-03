package vn.ssdc.vnpt.test.subscriber;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;
import vn.ssdc.vnpt.subscriber.services.SubscriberTemplateService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by THANHLX on 4/11/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestSubscriberService{
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void testGenerateDeviceId() {
        SubscriberService subscriberService = new SubscriberService(repositoryFactory);
        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "NV0001";
        Subscriber newSubscriber = subscriberService.create(subscriber);
        Assert.assertNotNull(newSubscriber.id);
    }

    @Test
    public void update() {
        SubscriberService subscriberService = new SubscriberService(repositoryFactory);

        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "subscriberId01";
        Subscriber subscriberCreate = subscriberService.create(subscriber);

        subscriberCreate.subscriberId = "subscriberId01edited";
        Subscriber subscriberUpdate = subscriberService.update(subscriberCreate.id, subscriberCreate);

        Assert.assertEquals(subscriberUpdate.subscriberId, "subscriberId01edited");
    }

    @Test
    public void delete() {
        Boolean pass = false;
        SubscriberService subscriberService = new SubscriberService(repositoryFactory);
        subscriberService.subscriberDeviceService = new SubscriberDeviceService(repositoryFactory);

        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "subscriberId01";
        Subscriber subscriberCreate = subscriberService.create(subscriber);

        subscriberService.delete(subscriberCreate.id);

        try {
            Subscriber subscriberGet = subscriberService.get(subscriberCreate.id);
            pass = !subscriberCreate.id.equals(subscriberGet.id);
        } catch (EntityNotFoundException e) {
            pass = true;
        } catch (Exception e) {
            pass = false;
        }

        Assert.assertTrue(pass);
    }

    @Test
    public void get() {
        SubscriberService subscriberService = new SubscriberService(repositoryFactory);

        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "subscriberId01";
        Subscriber subscriberCreate = subscriberService.create(subscriber);

        Subscriber subscriberGet = subscriberService.get(subscriberCreate.id);

        Assert.assertNotNull(subscriberGet.id);
    }

    @Test
    public void testFindBySubscribeId() {
        SubscriberService subscriberService = new SubscriberService(repositoryFactory);
        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "NV0002";
        subscriberService.create(subscriber);
        Subscriber newSubscriber = subscriberService.findBySubscribeId(subscriber.subscriberId);
        Assert.assertNotNull(newSubscriber.id);
    }

    @Test
    public void testFindBySubscriberTemplateId() {
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);
        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "Subscriber Template 1";
        Set<String> templateKeys = new HashSet<String>();
        templateKeys.add("key 1");
        templateKeys.add("key 2");
        subscriberTemplate.templateKeys = templateKeys;
        SubscriberTemplate newSubscriberTemplate = subscriberTemplateService.create(subscriberTemplate);

        SubscriberService subscriberService = new SubscriberService(repositoryFactory);
        Subscriber subscriber = new Subscriber();
        subscriber.subscriberId = "NV0002";
        Set<String> subscriberDataTemplateIds = new HashSet<String>();
        subscriberDataTemplateIds.add(String.valueOf(newSubscriberTemplate.id));
        subscriber.subscriberDataTemplateIds = subscriberDataTemplateIds;
        subscriberService.create(subscriber);
        Subscriber newSubscriber = subscriberService.findBySubscriberTemplateId(newSubscriberTemplate.id);
        Assert.assertNotNull(newSubscriber.id);
    }
}
