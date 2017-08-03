package vn.ssdc.vnpt.devices.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.umpexception.DuplicationDeviceTypeVersionException;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vietnq on 11/3/16.
 */
@Service
public class TagService extends SsdcCrudService<Long, Tag> {


    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;
    private Tag profileOthers;

    @Autowired
    public TagService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tag.class);
    }

    public List<Tag> findByDeviceTypeVersionIdAssignedSynchronized(Long deviceTypeVersionId){
        String whereExp = "device_type_version_id=? and assigned=0 and synchronize=1" ;
        return this.repository.search(whereExp, deviceTypeVersionId);
    }
    public List<Tag> findByDeviceTypeVersion(Long deviceTypeVersionId) {
        String whereExp = "device_type_version_id=? and assigned=0";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public List<Tag> findAssignedTags(Long deviceTypeVersionId) {
        String whereExp = "device_type_version_id=? and assigned=1";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public void deleteByRootTag(Long rootTagId, Long deviceTypeVersionId) {
        String query = "root_tag_id=? and device_type_version_id=?";
        List<Tag> listTag = this.repository.search(query, rootTagId, deviceTypeVersionId);
        if (!listTag.isEmpty()) {
            this.repository.delete(listTag.get(0).id);
        }
    }

//    public Tag findByDeviceTypeVersionAndName(Long deviceTypeVersionId, String path) {
//        String whereExp = "type='PARAMS' and assigned_group=3 and device_type_version_id=? and name=?";
//        List<Tag> listTag = this.repository.search(whereExp, deviceTypeVersionId, path);
//        return !listTag.isEmpty() ? listTag.get(0) : null;
//    }

    public void generateProfile(Map<String, Tag> listProfile, String profileNames, DeviceTypeVersion deviceTypeVersion) {
        if (profileNames.isEmpty() || profileNames.equals("")) {
            System.out.println("AAA");
        }
        Tag tag = new Tag();
        tag.name = profileNames;
        tag.deviceTypeVersionId = deviceTypeVersion.id;
        tag.parameters = new HashMap<String, Parameter>();
        tag.assigned = 0;
        tag.assignedGroup = "PROFILE";
        tag.rootTagId = null;
        listProfile.put(tag.name, tag);
    }

    public Tag generateProfileOther(String name, DeviceTypeVersion deviceTypeVersion) {
        Tag tag = new Tag();
        tag.name = name;
        tag.deviceTypeVersionId = deviceTypeVersion.id;
        tag.parameters = new HashMap<String, Parameter>();
        tag.assigned = 0;
        tag.assignedGroup = "PROFILE";
        tag.rootTagId = null;
        return tag;
    }

    public void addDeviceTypeVersionId(Long tagId, Long deviceTypeVersionId) {
        List<Tag> tagList = getListTagByRootTag(tagId);
        for (Tag tmp : tagList) {
            if (tmp.deviceTypeVersionId.equals(deviceTypeVersionId))
                throw new DuplicationDeviceTypeVersionException("DeviceTypeVersion " + deviceTypeVersionId + " existed!!!");
        }
        Tag tag = this.get(tagId);
        tag.id = null;
        tag.rootTagId = tagId;
        tag.deviceTypeVersionId = deviceTypeVersionId;
        this.create(tag);
    }

    public List<Tag> getListTagByRootTag(Long rootTag) {
        String query = "root_tag_id=?";
        List<Tag> listTag = this.repository.search(query, rootTag);
        return listTag;
    }

    public Boolean checkNameExisted(String tagName) {
        String query = "name=?";
        List<Tag> listTag = this.repository.search(query, tagName);
        if (listTag.isEmpty()) return false;
        return true;
    }

    public List<Tag> getListRootTag() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NULL AND root_tag_id IS NULL");
    }

    public List<Tag> getListAssigned() {
        return this.repository.search("assigned=1 AND device_type_version_id IS NOT NULL AND root_tag_id IS NOT NULL");
    }

    public List<Tag> getListProfiles() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NOT NULL AND root_tag_id IS NULL");
    }

    public List<Tag> getProvisioningTagByDeviceTypeVersionId(Long id) {
        String whereExp = "assigned=1 AND device_type_version_id=? AND root_tag_id IS NOT NULL";
        return this.repository.search(whereExp, id);
    }

    public Page<Tag> getPageRootTag(int page, int limit) {
        String whereExp = "assigned=0 AND device_type_version_id IS NULL AND root_tag_id IS NULL";
        return this.repository.search(whereExp, new PageRequest(page, limit));
    }

    public List<Tag> getListProvisioningTagByRootTagId(Long id) {
        String whereExp = "assigned=1 AND device_type_version_id IS NOT NULL AND root_tag_id=?";
        return this.repository.search(whereExp, id);
    }

    public List<Tag> findSynchronizedByDeviceTypeVersion(Long deviceTypeVersionId) {
        String whereExp = "synchronize=1 AND device_type_version_id=?";
        return this.repository.search(whereExp, deviceTypeVersionId);
    }

    public List<Tag> getListProfileSynchronized() {
        return this.repository.search("assigned=0 AND device_type_version_id IS NOT NULL AND root_tag_id IS NULL AND synchronize=1");
    }

    public Tag getProfileOthers(Long deviceTypeVersionId) {
        Tag tagResult = null;
        String whereExp = "name=? AND device_type_version_id=?";
        List<Tag> tags = this.repository.search(whereExp, "Others", deviceTypeVersionId);
        if (tags.size() > 0) {
            tagResult = tags.get(0);
        }

        return tagResult;
    }
}
