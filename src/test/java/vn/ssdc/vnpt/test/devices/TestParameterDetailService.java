package vn.ssdc.vnpt.test.devices;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestParameterDetailService {

    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void findParameters() {
        /*
        Boolean pass = false;

        ParameterDetailService parameterDetailService = new ParameterDetailService(repositoryFactory);

        ParameterDetail parameterDetail01 = new ParameterDetail();
        parameterDetail01.dataType = "object";
        ParameterDetail parameterDetail02 = new ParameterDetail();
        parameterDetail02.dataType = "int";
        parameterDetailService.create(parameterDetail01);
        parameterDetailService.create(parameterDetail02);

        List<ParameterDetail> parameterDetails = parameterDetailService.findParameters();
        if(!ObjectUtils.empty(parameterDetails) && parameterDetails.size() == 1) {
            for (ParameterDetail _parameterDetail : parameterDetails) {
                if ("object".equals(_parameterDetail.dataType)) {
                    break;
                }
            }
            pass = true;
        }

        Assert.assertTrue(pass);
        */
    }
}
