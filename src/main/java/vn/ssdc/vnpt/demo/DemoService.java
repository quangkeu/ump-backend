package vn.ssdc.vnpt.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

/**
 * Created by vietnq on 10/31/16.
 */
@Service
public class DemoService extends SsdcCrudService<Long,Demo> {
    @Autowired
    public DemoService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Demo.class);
    }
}
