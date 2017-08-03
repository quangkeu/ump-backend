package vn.ssdc.vnpt.policy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.List;

/**
 * Created by Admin on 3/13/2017.
 */
@Service
public class PolicyTaskService extends SsdcCrudService<Long, PolicyTask> {
    @Autowired
    public PolicyTaskService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PolicyTask.class);
    }

    public PolicyTask findByTaskId(String taskId){
        String whereExp = "task_id = ?" ;
        List<PolicyTask> policyTasks = this.repository.search(whereExp, taskId);
        if(policyTasks.isEmpty()) {
            return null;
        }
        else {
            return policyTasks.get(0);
        }
    }

    public long count(String query) {
        return this.repository.count(query);

    }

    public List<PolicyTask> groupByStatus(){
        return this.repository.searchWithGroupBy("status");
    }
}
