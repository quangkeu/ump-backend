package vn.ssdc.vnpt.subscriber.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import javax.ws.rs.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Huy Hieu on 11/24/2016.
 */
@Service
public class SubscriberService extends SsdcCrudService<Long, Subscriber> {

    public static final String SUBSCRIBER_ID_COLUMN = "subscriber_id";
    private static final Logger logger = LoggerFactory.getLogger(SubscriberService.class);

    @Autowired
    private AcsClient acsClient;

    @Autowired
    public DeviceTypeService deviceTypeService;

    @Autowired
    public SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public SubscriberTemplateService subscriberTemplateService;

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public TagService tagService;

    @Autowired
    public SubscriberService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Subscriber.class);
    }

    public Subscriber findBySubscribeId(String subscriberId) {
        String whereExp = SUBSCRIBER_ID_COLUMN + "=?";
        List<Subscriber> subscribers = this.repository.search(whereExp, subscriberId);
        if (!ObjectUtils.empty(subscribers)) {
            return subscribers.get(0);
        }
        return null;

    }


    public Subscriber findBySubscriberTemplateId(Long subscriberDataTemplateId) {
        String whereExp = "subscriber_data_template_ids LIKE ?";
        List<Subscriber> subscribers = this.repository.search(whereExp, "%"+subscriberDataTemplateId+"%");
        if (!ObjectUtils.empty(subscribers)) {
            return subscribers.get(0);
        }
        return null;
    }

    @Override
    public void beforeDelete(Long id) {
        Subscriber subscriber = this.get(id);
        if (!ObjectUtils.empty(subscriber)) {
            List<SubscriberDevice> subscriberDevice = subscriberDeviceService.findBySubscribeId(subscriber.subscriberId);
            if (!ObjectUtils.empty(subscriberDevice) && subscriberDevice.isEmpty()) {
                throw new NotFoundException("Cannot delete.");
            }
        } else {
            throw new NotFoundException("Cannot delete.");
        }
    }

    public Page<Subscriber> getPage(int page, int limit) {
        return this.repository.findAll(new PageRequest(page, limit));
    }

    public Set<String> validateSubscriber(Subscriber subscriber) {

        Set<String> messages = new LinkedHashSet<String>();

        // subscriberId
        if (subscriber.subscriberId != null) {
            // length:20
            if (subscriber.subscriberId.length() > 20) {
                messages.add("subscriberId length < 20.");
            }

            // special character
            Pattern p = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(subscriber.subscriberId);
            if (m.find()) {
                messages.add("subscriberId has special character.");
            }

            // exist
            if (findBySubscribeId(subscriber.subscriberId) != null) {
                messages.add("subscriberId is existed.");
            }

        } else {
            // required
            messages.add("subscriberId is required.");
        }

        // subscriberDataTemplateIds
        Set<SubscriberTemplate> subscriberTemplates = new LinkedHashSet<SubscriberTemplate>();
        Set<String> subscriberDataTemplates = new LinkedHashSet<String>();
        try {
            for (String subscriberTemplateId : subscriber.subscriberDataTemplateIds) {
                SubscriberTemplate subscriberTemplate = subscriberTemplateService.get(Long.valueOf(subscriberTemplateId));
                if (subscriberTemplate != null) {
                    subscriberTemplates.add(subscriberTemplate);
                    subscriberDataTemplates.addAll(subscriberTemplate.templateKeys);
                }
            }
        } catch (EntityNotFoundException e) {
            messages.add("subscriberDataTemplateIds not exist.");
        }

        // subscriberData
        for(String subscriberDataKey: subscriber.subscriberData.keySet()) {
            if(!subscriberDataTemplates.contains(subscriberDataKey)) {
                messages.add("subscriberData "+subscriberDataKey+" not exist.");
            }
        }

        // deviceIds
        Pattern p = Pattern.compile("[^a-z0-9-]", Pattern.CASE_INSENSITIVE);
        for(String deviceId: subscriber.getDeviceIds()) {

            // special character
            Matcher m = p.matcher(deviceId);
            if (m.find()) {
                messages.add("deviceId " + deviceId + " has special character.");
            }

            // format oui-productClass-serialNumber
            if(deviceId.split("-").length < 3) {
                messages.add("deviceId " + deviceId + " valid format.");
            }

            // is used
            if(subscriberDeviceService.findByDeviceId(deviceId).size() > 0) {
                messages.add("deviceId " + deviceId + " is used.");
            }
        }

        return messages;
    }

    public Subscriber postCreate(Subscriber subscriber) {

        // Save subscriber
        subscriber = create(subscriber);

        // Save subscriber device
        for (String deviceId: subscriber.getDeviceIds()) {

            SubscriberDevice subscriberDevice = new SubscriberDevice();
            subscriberDevice.deviceId = deviceId;
            subscriberDevice.subscriberId = subscriber.subscriberId;

            ResponseEntity<String> deviceInfo = acsClient.getDevice(deviceId, "_deviceId._Manufacturer,_deviceId._OUI,_deviceId._ProductClass,_deviceId._SerialNumber");
            JsonArray deviceObject = new Gson().fromJson(deviceInfo.getBody(), JsonArray.class);
            if(deviceObject.size() > 0) {
                JsonObject element = (JsonObject) deviceObject.get(0);
                subscriberDevice.manufacturer = element.get("summary.manufacturer").getAsString();
                subscriberDevice.oui = element.get("summary.oui").getAsString();
                subscriberDevice.productClass = element.get("summary.productClass").getAsString();
                subscriberDevice.serialNumber = element.get("summary.serialNumber").getAsString();
            } else {
                String[] deviceIds = deviceId.split("-");
                subscriberDevice.oui = deviceIds[0];
                subscriberDevice.productClass = deviceIds[1];
                subscriberDevice.serialNumber = deviceIds[2];
            }

            subscriberDeviceService.create(subscriberDevice);
        }

        return subscriber;
    }
}
