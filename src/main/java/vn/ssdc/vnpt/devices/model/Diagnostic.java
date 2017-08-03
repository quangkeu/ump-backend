package vn.ssdc.vnpt.devices.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by SSDC on 11/15/2016.
 */
public class Diagnostic extends SsdcEntity<String> {

    public static final String PING_INTERFACE_LIST = "InternetGatewayDevice.Layer2Bridging.AvailableInterface";

    public String _id;

    public Map<String, String> parameters = new HashMap<String, String>(); //holds device (parameter,value) mapping

    public static List<String> getListInterface(String inputFromSearchResult) {
        JsonArray array = new Gson().fromJson(inputFromSearchResult, JsonArray.class);
        JsonObject object1 = array.get(0).getAsJsonObject();
        JsonObject object2 = object1.get("InternetGatewayDevice").getAsJsonObject().get("Layer2Bridging").getAsJsonObject().get("AvailableInterface").getAsJsonObject();
        List<String> lstInterface = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : object2.entrySet()) {
            JsonElement tmp = entry.getValue();
            if (tmp.isJsonObject()) {
                String interfaceDevice = tmp.getAsJsonObject().get("InterfaceReference").getAsJsonObject().get("_value").getAsString();
                lstInterface.add(interfaceDevice);
            }
        }
        return lstInterface;

    }
}
