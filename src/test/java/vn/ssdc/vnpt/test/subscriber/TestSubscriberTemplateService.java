package vn.ssdc.vnpt.test.subscriber;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;
import vn.ssdc.vnpt.subscriber.services.SubscriberTemplateService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestSubscriberTemplateService {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void create() {
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);

        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "name01";
        SubscriberTemplate subscriberTemplateCreate = subscriberTemplateService.create(subscriberTemplate);

        Assert.assertNotNull(subscriberTemplateCreate.id);
    }

    @Test
    public void update() {
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);

        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "name01";
        SubscriberTemplate subscriberTemplateCreate = subscriberTemplateService.create(subscriberTemplate);

        subscriberTemplateCreate.name = "name01edited";
        SubscriberTemplate subscriberTemplateUpdate = subscriberTemplateService.update(subscriberTemplateCreate.id, subscriberTemplateCreate);

        Assert.assertEquals(subscriberTemplateUpdate.name, "name01edited");
    }

    @Test
    public void delete() {
        Boolean pass = false;
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);
        subscriberTemplateService.subscriberService = new SubscriberService(repositoryFactory);

        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "name01";
        SubscriberTemplate subscriberTemplateCreate = subscriberTemplateService.create(subscriberTemplate);

        subscriberTemplateService.delete(subscriberTemplateCreate.id);

        try {
            SubscriberTemplate subscriberTemplateGet = subscriberTemplateService.get(subscriberTemplateCreate.id);
            pass = !subscriberTemplateCreate.id.equals(subscriberTemplateGet.id);
        } catch (EntityNotFoundException e) {
            pass = true;
        } catch (Exception e) {
            pass = false;
        }

        Assert.assertTrue(pass);
    }

    @Test
    public void get() {
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);

        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "name01";
        SubscriberTemplate subscriberTemplateCreate = subscriberTemplateService.create(subscriberTemplate);

        SubscriberTemplate subscriberTemplateGet = subscriberTemplateService.get(subscriberTemplateCreate.id);

        Assert.assertNotNull(subscriberTemplateGet.id);
    }

    @Test
    public void getPage() {
        SubscriberTemplateService subscriberTemplateService = new SubscriberTemplateService(repositoryFactory);

        SubscriberTemplate subscriberTemplate = new SubscriberTemplate();
        subscriberTemplate.name = "name01";
        subscriberTemplateService.create(subscriberTemplate);

        Page<SubscriberTemplate> subscriberTemplatePage = subscriberTemplateService.getPage(1, 1);

        Assert.assertNotNull(subscriberTemplatePage.getTotalElements());
    }
}
