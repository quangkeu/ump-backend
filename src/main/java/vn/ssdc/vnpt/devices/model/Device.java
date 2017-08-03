package vn.ssdc.vnpt.devices.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Admin on 3/14/2017.
 */
public class Device {
    private static final Logger logger = LoggerFactory.getLogger(Device.class);
    public String id;
    public Map<String, List<Tag>> tags = new HashMap<String, List<Tag>>();
    public Map<String, String> parameters = new HashMap<String, String>(); //holds device (parameter,value) mapping

    private static final String VALUE_KEY = "_value";

    public String serialNumber() {
        return parameters.get("_deviceId._SerialNumber");
    }

    public String manufacturer() {
        return parameters.get("_deviceId._Manufacturer");
    }

    public String oui() {
        return parameters.get("_deviceId._OUI");
    }

    public String productClass() {
        return parameters.get("_deviceId._ProductClass");
    }

    public String softwareVersion() {
        return parameters.get("summary.softwareVersion");
    }

    public static List<Device> fromJsonString(String queryResult, Set<String> paramNames) {
        List<Device> list = new ArrayList<Device>();
        JsonArray array = new Gson().fromJson(queryResult, JsonArray.class);
        for (int i = 0; i < array.size(); i++) {
            list.add(fromJsonObject(array.get(i).getAsJsonObject(), paramNames));
        }
        return list;
    }

    public static Device fromJsonObject(JsonObject deviceObject, Set<String> paramNames) {
        Device device = new Device();
        device.id = deviceObject.get("_id").getAsString();
        for (String param : paramNames) {
            device.parameters.put(param, getParamValue(deviceObject, param));
            if ("_registered".equalsIgnoreCase(param) || "_lastInform".equalsIgnoreCase(param) || "_timestamp".equalsIgnoreCase(param)) {
                if(!getParamValue(deviceObject, param).isEmpty()) {
                    device.parameters.put(param, DateUtils.convertIsoDateToString(getParamValue(deviceObject, param)));
                }
            }
            if ("_ip".equalsIgnoreCase(param)) {
                if (getParamValue(deviceObject, param) != null && !"".equals(getParamValue(deviceObject, param)))
                    device.parameters.put(param, convertIntToIpAddres(Integer.valueOf(getParamValue(deviceObject, param))));
                else
                    device.parameters.put(param, "");
            }
        }
        return device;
    }

    private static String getParamValue(JsonObject deviceObject, String paramName) {
        String value = "";
        try {
            if (paramName.startsWith("summary")) {
                try {
                    deviceObject = deviceObject.get(paramName).getAsJsonObject();
                } catch (Exception e) {
//                    logger.error("{}", e);
                    if (deviceObject.get(paramName) != null) {
                        value = deviceObject.get(paramName).getAsString();
                    } else {
                        value = "";
                    }
                }
            } else {
                String[] parts = paramName.split("\\."); //need escape to have correct regex here
                for (int i = 0; i < parts.length; i++) {
                    try {
                        deviceObject = deviceObject.getAsJsonObject(parts[i]);
                    } catch (Exception e) {
                        value = deviceObject.get(parts[i]).getAsString();
                        logger.error(e.getMessage());
                    }
                }
            }

            if ("".equals(value)) {
                if (deviceObject != null && deviceObject.has(VALUE_KEY)) {
                    value = deviceObject.get(VALUE_KEY).getAsString();
                }
                if (paramName.endsWith(".")) {
                    value = new Gson().toJson(deviceObject);
                }
            }
        } catch (Exception e) {
            logger.error("Error when getting value for param: {}, exception: {}", paramName, e.getCause());
            try {
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return value;
    }

    public static String convertIntToIpAddres(int ip) {
        return ((ip >> 24) & 0xFF) + "." +

                ((ip >> 16) & 0xFF) + "." +

                ((ip >> 8) & 0xFF) + "." +

                (ip & 0xFF);
    }
}
