package vn.ssdc.vnpt.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.user.model.Operation;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Lamborgini on 5/9/2017.
 */
@Service
public class OperationService extends SsdcCrudService<String, Operation> {

    @Autowired
    public OperationService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Operation.class);
    }

    public boolean getById(String id) {
        int count = this.repository.search("id=?", id).size();
        if (count == 0) {
            return false;
        }
        return true;
    }

}
