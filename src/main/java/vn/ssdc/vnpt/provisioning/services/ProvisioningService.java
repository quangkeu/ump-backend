package vn.ssdc.vnpt.provisioning.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.devices.services.DeviceTypeService;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.*;


@Service
public class ProvisioningService extends SsdcCrudService<Long, Tag> {


    public static final int ACTION_PRESET_CREATEORUPDATE = 1;
    public static final int ACTION_PRESET_DELETE = 0;

    private static final String KEY_PRESET = "preset_";

    @Autowired
    public AcsClient acsClient;

    @Autowired
    public DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeService deviceTypeService;

    @Autowired
    public TagService tagService;

    @Autowired
    public SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public ParameterDetailService parameterDetailService;

    @Autowired
    public ProvisioningService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tag.class);
    }

    public void createProvisioningTasks(String deviceId) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            JsonObject body = arrayTmpObject.get(0).getAsJsonObject();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String productClass = inforObject.get("_ProductClass") != null ? inforObject.get("_ProductClass").getAsString() : "";
            String oui = inforObject.get("_OUI").getAsString() != null ? inforObject.get("_OUI").getAsString() : "";
            String firmwareVersion = body.get("summary.softwareVersion") != null ? body.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString() : "";
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            if (currenDeviceType != null) {
                DeviceTypeVersion currentDeviceTypeVersion = deviceTypeVersionService.findByPk(currenDeviceType.id, firmwareVersion);
                if (currentDeviceTypeVersion != null) {
                    List<Tag> listProvisioningTags = tagService.getProvisioningTagByDeviceTypeVersionId(currentDeviceTypeVersion.id);
                    Map<String, Object> parameterValues = new HashMap<String, Object>();
                    for (Tag provisioningTag : listProvisioningTags) {
                        Map<String, Parameter> parameterMap = provisioningTag.parameters;
                        List<String> listPath = new ArrayList<String>(provisioningTag.parameters.keySet());
                        String parameters = StringUtils.join(listPath, ",");
                        ResponseEntity<String> responseEntity = acsClient.getDevice(deviceId, parameters);
                        String responseEntityBody = responseEntity.getBody();
                        List<Device> devices = Device.fromJsonString(responseEntityBody, provisioningTag.parameters.keySet());
                        if (devices.size() > 0) {
                            Map<String, String> listParametersOfDevice = devices.get(0).parameters;
                            //List of change parameters
                            for (Map.Entry<String, Parameter> entry : parameterMap.entrySet()) {
                                String path = entry.getKey();
                                Parameter parameter = entry.getValue();
                                String valueOfDevice = listParametersOfDevice.get(path);
                                String provisioningValue = getProvisioningValue(deviceId, parameter);
                                if (valueOfDevice != null && provisioningValue != null) {
                                    if (!provisioningValue.equals(valueOfDevice)) {
                                        parameterValues.put(path, provisioningValue);
                                    }
                                }
                            }
                            //List of new objects
                            //tuanha2
                            //Sort ListPath
                            Collections.sort(listPath);
                            //
                            Map<String, String> lstParameterDetail = new HashMap<String, String>();
                            //step.2 Check instance
                            String strLastPath = "";
                            String strValue = "";
                            //
                            for (Iterator<String> iter = listPath.listIterator(); iter.hasNext(); ) {
                                String strPath = iter.next();
                                String[] args = strPath.split("\\.");
                                try {
                                    String strInstance = args[args.length - 2];
                                    String strShortName = args[args.length - 1];

                                    Integer.parseInt(strInstance);
                                    //step.3 Get objectname
                                    String strTemp = "";
                                    for (int index = 0; index < (args.length - 2); index++) {
                                        strTemp += args[index] + ".";
                                    }
                                    if (!strLastPath.equals(strTemp) && !strLastPath.isEmpty()) {
                                        strValue = strValue.substring(0, strValue.length() - 1);
                                        lstParameterDetail.put(strLastPath, strValue);
                                        strLastPath = strTemp;
                                        strValue = "";
                                        strValue += strPath + "$" + strShortName + ",";
                                    } else if ((iter.hasNext() == true && strLastPath.equals(strTemp)) || strLastPath.isEmpty()) {
                                        strLastPath = strTemp;
                                        strValue += strPath + "$" + strShortName + ",";
                                    } else {
                                        strValue = strValue.substring(0, strValue.length() - 1);
                                        lstParameterDetail.put(strLastPath, strValue);
                                    }
                                } catch (Exception ex) {
                                    //Not Instance Then Remove
                                    iter.remove();
                                }
                            }

                            for (Map.Entry<String, String> entry : lstParameterDetail.entrySet()) {
                                System.out.println(entry.getKey() + "/" + entry.getValue());
                                String[] argParam = entry.getValue().split("\\,");
                                //step.4 Check objectName
                                // - find by path (access true)
                                // - get parent object then
                                List<ParameterDetail> lstParam = parameterDetailService.findForProvisioning(entry.getKey());
                                Map<String, String> mapAddObject = new HashMap<String, String>();

                                for (int size = 0; size < argParam.length; size++) {
                                    String strOriginPath = argParam[size].split("\\$")[0];
                                    String strOriginShortName = argParam[size].split("\\$")[1];
                                    for (int index = 0; index < lstParam.size(); index++) {
                                        ParameterDetail parameterDetail = lstParam.get(index);
                                        if (parameterDetail.shortName.equals(strOriginShortName)) {
                                            //getValue;
                                            String provisioningValue = getProvisioningValue(deviceId, parameterMap.get(strOriginPath));
                                            if (provisioningValue != null) {
                                                mapAddObject.put(strOriginShortName, getProvisioningValue(deviceId, parameterMap.get(strOriginPath)));
                                            }
                                            break;
                                        }
                                    }
                                }
                                // step.5 Add Object
                                if(mapAddObject.size()>0){
                                    acsClient.addObject(deviceId, entry.getKey(), mapAddObject, true);
                                }
                            }
                        }
                    }
                    if (parameterValues.size() > 0) {
                        acsClient.setParameterValues(deviceId, parameterValues, false);
                    }
                }
            }
        }
    }

    public String getProvisioningValue(String deviceId, Parameter parameter) {
        if (parameter.useSubscriberData != null && parameter.useSubscriberData == 1) {
            List<Subscriber> subscribers = subscriberDeviceService.findByDeviceId(deviceId);
            if (subscribers.size() > 0) {
                Subscriber subscriber = subscribers.get(0);
                String subscriberDataKey = parameter.subscriberData;
                if (subscriber.subscriberData.get(subscriberDataKey) != null) {
                    return subscriber.subscriberData.get(subscriberDataKey);
                }
            }
        }
        if (parameter.value != null && !parameter.value.isEmpty()) {
            return parameter.value;
        }
        return parameter.defaultValue;
    }
}
