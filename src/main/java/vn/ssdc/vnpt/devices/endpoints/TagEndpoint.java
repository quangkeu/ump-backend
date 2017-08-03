package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by vietnq on 11/3/16.
 */
@Component
@Path("tags")
@Produces(APPLICATION_JSON)
@Api("Tags")
public class TagEndpoint extends SsdcCrudEndpoint<Long,Tag> {

    private TagService tagService;

    @Autowired
    public TagEndpoint(TagService tagService) {
        this.service = this.tagService = tagService;
    }

    @Autowired
    public DataModelService dataModelService;

    @GET
    @Path("/find-by-device-type-version")
    public List<Tag> findByDeviceTypeVersion(@QueryParam("id") Long id) {
        return tagService.findByDeviceTypeVersion(id);
    }

    @GET
    @Path("/get-list-root-tag")
    public List<Tag> getListRootTag() {
        return tagService.getListRootTag();
    }

    @GET
    @Path("/get-provisioning-tag-by-device-type-version-id")
    public List<Tag> getRootTagByDeviceTypeVersionId(@QueryParam("id") Long id) {
        return tagService.getProvisioningTagByDeviceTypeVersionId(id);
    }

    @GET
    @Path("/get-list-provisioning-tag-by-root-tag-id")
    public List<Tag> getListProvisioningTagByRootTagId(@QueryParam("id") Long id) {
        return tagService.getListProvisioningTagByRootTagId(id);
    }

    @GET
    @Path("/get-list-assigned")
    public List<Tag> getListAssigned() {
        return tagService.getListAssigned();
    }

    @GET
    @Path("/get-list-profiles")
    public List<Tag> getListProfiles() {
        return tagService.getListProfiles();
    }

    @GET
    @Path("/get-page-root-tag")
    public Page<Tag> getPageRootTag(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("limit") @DefaultValue("20") int limit) {
        return tagService.getPageRootTag(page, limit);
    }

    @GET
    @Path("/find-synchronized-by-device-type-version")
    public List<Tag> findSynchronizedByDeviceTypeVersion(@QueryParam("id") @DefaultValue("0") Long deviceTypeVersionId) {
        return tagService.findSynchronizedByDeviceTypeVersion(deviceTypeVersionId);
    }

    @GET
    @Path("/get-parameters-of-device/{tagId}/{deviceId}")
    public Set<Parameter> getParametersOfDevice(@PathParam("tagId") Long tagId, @PathParam("deviceId") String deviceId) {
        return dataModelService.getProfileOfDevices(deviceId, tagId);
    }

    @GET
    @Path("/get-list-profile-synchronized")
    public List<Tag> getListProfileSynchronized() {
        return tagService.getListProfileSynchronized();
    }

    @GET
    @Path("/get-profile-others")
    public Tag getProfileOthers(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId) {
        return tagService.getProfileOthers(deviceTypeVersionId);
    }
}
