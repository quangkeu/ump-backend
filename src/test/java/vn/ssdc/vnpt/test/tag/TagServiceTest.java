package vn.ssdc.vnpt.test.tag;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.test.UmpTestConfiguration;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Admin on 4/12/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=UmpTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TagServiceTest {
    @Autowired
    RepositoryFactory repositoryFactory;

    @Test
    public void findByDeviceTypeVersionIdAssignedSynchronized() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.deviceTypeVersionId = 1l;
        tag.assigned = 0;
        tag.synchronize = 1;
        tagService.create(tag);

        Assert.assertNotNull(tagService.findByDeviceTypeVersionIdAssignedSynchronized(1l));
    }

    @Test
    public void findByDeviceTypeVersion() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.deviceTypeVersionId = 1l;
        tag.assigned = 0;
        tagService.create(tag);
        Assert.assertNotNull(tagService.findByDeviceTypeVersion(1l));
    }

    @Test
    public void findAssignedTags() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.deviceTypeVersionId = 1l;
        tag.assigned = 1;
        tagService.create(tag);
        Assert.assertNotNull(tagService.findAssignedTags(1l));
    }

    @Test
    public void deleteByRootTag() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.rootTagId = 1l;
        tag.deviceTypeVersionId = 1l;
        tagService.create(tag);

        tagService.deleteByRootTag(1l,1l);
        Assert.assertTrue(tagService.getListRootTag().size()==0);
    }

    @Test
    public void generateProfile() throws Exception {
        Map<String, Tag> listProfile  = new HashMap<String, Tag>();
        TagService tagService = new TagService(repositoryFactory);
        //
        Tag tag = new Tag();
        //
        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.id = 1l;
        //
        tagService.generateProfile(listProfile,"test",deviceTypeVersion);
        Assert.assertNotNull(listProfile);
    }

    @Test
    public void generateProfileOther() throws Exception {
        TagService tagService = new TagService(repositoryFactory);

        DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
        deviceTypeVersion.id = 1l;
        //
        Tag tag = tagService.generateProfileOther("test" , deviceTypeVersion);
        //
        Assert.assertNotNull(tag);
    }

    @Test
    public void addDeviceTypeVersionId() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.deviceTypeVersionId=1l;
        tagService.create(tag);
        //
        tagService.addDeviceTypeVersionId(tag.id,2l);
        Assert.assertNotNull(tagService.get(tag.id));
        //
    }

    @Test
    public void getListTagByRootTag() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.rootTagId = 1l;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getListTagByRootTag(1l));
        //
    }

    @Test
    public void checkNameExisted() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.name = "test";
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.checkNameExisted("test"));
        //
    }

    @Test
    public void getListRootTag() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 0;
        tag.deviceTypeVersionId = null;
        tag.rootTagId = null;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getListRootTag());
        //
    }

    @Test
    public void getListAssigned() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 1;
        tag.deviceTypeVersionId = 1l;
        tag.rootTagId = 1l;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getListAssigned());
        //
    }

    @Test
    public void getListProfiles() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 0;
        tag.deviceTypeVersionId = 1l;
        tag.rootTagId = null;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getListProfiles());
    }

    @Test
    public void getProvisioningTagByDeviceTypeVersionId() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 1;
        tag.deviceTypeVersionId = 1l;
        tag.rootTagId = 1l;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getProvisioningTagByDeviceTypeVersionId(tag.id));
    }

    @Test
    public void getPageRootTag() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 0;
        tag.deviceTypeVersionId = null;
        tag.rootTagId = null;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getPageRootTag(1,1));
    }

    @Test
    public void getListProvisioningTagByRootTagId() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.assigned = 1;
        tag.deviceTypeVersionId = 1l;
        tag.rootTagId = 1l;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.getListProvisioningTagByRootTagId(tag.id));
    }

    @Test
    public void findSynchronizedByDeviceTypeVersion() throws Exception {
        TagService tagService = new TagService(repositoryFactory);
        Tag tag = new Tag();
        tag.deviceTypeVersionId=1l;
        tagService.create(tag);
        //
        Assert.assertNotNull(tagService.findSynchronizedByDeviceTypeVersion(1l));
    }

}