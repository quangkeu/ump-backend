package vn.ssdc.vnpt.subscriber.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.NotFoundException;

@Service
public class SubscriberTemplateService extends SsdcCrudService<Long, SubscriberTemplate> {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberTemplate.class);

    @Autowired
    public SubscriberService subscriberService;

    @Autowired
    public SubscriberTemplateService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(SubscriberTemplate.class);
    }

    @Override
    public void beforeDelete(Long id) {
        Subscriber subscriber = subscriberService.findBySubscriberTemplateId(id);
        if(!ObjectUtils.empty(subscriber)) {
            throw new NotFoundException("Cannot delete");
        }
    }

    public Page<SubscriberTemplate> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }
}