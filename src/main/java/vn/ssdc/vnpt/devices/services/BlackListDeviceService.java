package vn.ssdc.vnpt.devices.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.BlacklistDevice;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.core.SsdcCrudService;

import java.util.List;

/**
 * Created by Lamborgini on 3/3/2017.
 */
@Service
public class BlackListDeviceService extends SsdcCrudService<Long, BlacklistDevice> {

    @Autowired
    public BlackListDeviceService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(BlacklistDevice.class);
    }

    public List<BlacklistDevice> findByDeviceId(String deviceId){
        String whereExp = "device_id=?" ;
        return this.repository.search(whereExp, deviceId);
    }

}
