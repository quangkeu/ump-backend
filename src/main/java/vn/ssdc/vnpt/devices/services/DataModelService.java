package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.*;
import vn.ssdc.vnpt.umpexception.DeviceNotFoundException;
import vn.ssdc.vnpt.umpexception.DuplicationFirmwareVersionException;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kiendt on 1/23/2017.
 */

@Service
public class DataModelService extends SsdcCrudService<Long, DeviceTypeVersion> {
    private static final Logger logger = LoggerFactory.getLogger(DataModelService.class);

    private static final String SOFTWARE_KEY = "summary.softwareVersion";
    private static final String MODELNAME_KEY = "summary.modelName";
    private static final String PRODUCTCLASS_KEY = "_ProductClass";
    private static final String MANUFACTURE_KEY = "_Manufacturer";
    private static final String OUI_KEY = "_OUI";
    private static final String VALUE_KEY = "_value";
    private static final String OBJECT_KEY = "_object";
    private static final String WRIABLE_KEY = "_writable";
    private static final String TYPE_KEY = "_type";
    private static final String INSTANCE_KEY = "_instance";
    private static final String PROFILE_OTHER = "Others";
    private static final String PROFILE_VENDOR = "Vendor";


    List<String> ignoredParam = Arrays.asList(new String[]{"_id", "_registered", "_deviceId", "_lastInform", "_ip", "_lastBoot", "_lastBootstrap", "_timestamp", "_writable", "summary.lastInform"});
    @Autowired
    private DeviceTypeService deviceTypeService;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    private ParameterDetailService parameterDetailService;

    @Autowired
    private Tr069ParameterService tr069ParameterService;

    @Autowired
    private TagService tagService;


    @Autowired
    public DataModelService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DeviceTypeVersion.class);
    }

    @Autowired
    private Tr069ProfileService tr069ProfileService;

    @Value("${tmpDir}")
    private String tmpDir;

    public void setAcsClient(AcsClient acsClient) {
        this.acsClient = acsClient;
    }

    /**
     * get Infor of deviceId from genies
     *
     * @param deviceId
     * @return
     */
    public JsonObject getInforDevice(String deviceId) {
        Map<String, String> acsQuery = new HashMap<String, String>();
        acsQuery.put("query", "{\"_id\":\"" + deviceId + "\"}");
        JsonArray arrayTmpObject = new Gson().fromJson(acsClient.search("devices", acsQuery).getBody(), JsonArray.class);
        if (arrayTmpObject.size() > 0) {
            return arrayTmpObject.get(0).getAsJsonObject();
        }
        throw new DeviceNotFoundException("Cannot find infor about deviceId " + deviceId);
    }

    /**
     * API export XML for data model
     *
     * @param deviceTypeVersionId
     */
    public String exportDataModelJson(Long deviceTypeVersionId) {
        String strReturn = "ERROR EXPORT !";
        DeviceTypeVersion deviceTypeVersion = deviceTypeVersionService.get(deviceTypeVersionId);

        Map<String, String> mapData = new HashMap<String, String>();
        Map<String,ParameterDetail> listParameter = parameterDetailService.findByDeviceTypeVersion(deviceTypeVersionId);
        for (String key : listParameter.keySet()) {
            ParameterDetail parameter = listParameter.get(key);
            String strValue;
            if (!"object".equals(parameter.dataType)) {
                strValue = "[" + parameter.access + ",\"" + parameter.defaultValue + "\"," + "\"xsd:" + parameter.dataType + "\"]";
            }
            else{
                strValue = "[" + parameter.access + "]";
            }
            mapData.put(key, strValue);
        }

        String strTimeCreated = String.valueOf(System.currentTimeMillis());
        File jsonFile = new File(tmpDir + "/" + deviceTypeVersion.modelName + "_" + strTimeCreated + ".json");

        List sortedKeys = new ArrayList(mapData.keySet());
        Collections.sort(sortedKeys);

        try {
            if (jsonFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(jsonFile);
                PrintWriter pw = new PrintWriter(fos);
                pw.println("{");
                for (int index = 0; index < sortedKeys.size(); index++) {
                    if(index == (sortedKeys.size()-1)){
                        pw.println("\"" + sortedKeys.get(index) + "\"" + " : " + mapData.get(sortedKeys.get(index)));
                    }
                    else {
                        pw.println("\"" + sortedKeys.get(index) + "\"" + " : " + mapData.get(sortedKeys.get(index)) + ",");
                    }
                }
                pw.println("}");
                pw.flush();
                pw.close();
                fos.close();
            }
            strReturn = jsonFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            strReturn += e;
        }
        return strReturn;
    }

    public String exportDataModelXML(String deviceTypeVersionId) {
        String strReturn = "ERROR EXPORT ! ";
        try {
            //1st Create XML
            Document document = DocumentHelper.createDocument();
            //Set Root
            Element root = document.addElement("model");
            //2st Get All Object Parameter Detail With deviceTypeVersionId
            List<ParameterDetail> listObject = parameterDetailService.getAllObject(deviceTypeVersionId);
            //3st Get Parameter
            for (int intIndex = 0; intIndex < listObject.size(); intIndex++) {
                ParameterDetail Object = listObject.get(intIndex);
                String strPath = Object.path;

                Element eObject = root.addElement("object").addAttribute("name", strPath);
                //After get Object then get All Their Parameter
                List<ParameterDetail> listParameter = parameterDetailService.getAllParameter(deviceTypeVersionId, strPath);
                for (int intIndexParameter = 0; intIndexParameter < listParameter.size(); intIndexParameter++) {
                    ParameterDetail parameter = listParameter.get(intIndexParameter);
                    if (!"object".equalsIgnoreCase(parameter.dataType)) {
                        //Process cut string parameter
                        String strParamter = parameter.path;
                        Element eParameter = eObject.addElement("parameter").addAttribute("name", strParamter.substring(strPath.length(), strParamter.length()));
                        //
                        if (parameter.description != null) {
                            Element eDes = eParameter.addElement("description");
                            eDes.addText(parameter.description);
                        }
                        //
                        Element eSyntax = eParameter.addElement("syntax");
                        if (!"".equals(parameter.defaultValue)) {
                            Element eDataType = eSyntax.addElement(parameter.dataType);
                            eDataType.addElement("defaultValue").addText(parameter.defaultValue);
                        } else {
                            eSyntax.addElement(parameter.dataType);
                        }
                    }
                }
            }
            // Pretty print the document to System.out
            OutputFormat format = OutputFormat.createPrettyPrint();
            String strTimeCreated = String.valueOf(System.currentTimeMillis());
            File xmlFile = new File(tmpDir + "/datamodel_" + strTimeCreated + ".xml");
            if (xmlFile.createNewFile()) {
                XMLWriter output = new XMLWriter(new FileWriter(xmlFile), format);
                output.write(document);
                output.close();
            }
            strReturn = xmlFile.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            logger.error("{}", e);
//            e.printStackTrace();
            strReturn += e;
        } catch (IOException e) {
            logger.error("{}", e);
//            e.printStackTrace();
            strReturn += e;
        }
//        System.out.println(strReturn);
        return strReturn;
    }

    public Set<Parameter> getProfileOfDevices(String deviceId, Long tagId){
        Tag tag = tagService.get(tagId);
        JsonObject body = getInforDevice(deviceId);
        List<String> listTr069Names = new ArrayList<String>();
        for (Map.Entry<String, Parameter> entry : tag.parameters.entrySet())
        {
            String tr069Name = entry.getValue().tr069Name;
            if(!listTr069Names.contains(tr069Name)) {
                listTr069Names.add(tr069Name);
            }
            int indexOf = tr069Name.indexOf(".");
            while (indexOf >= 0) {
                String parameterPath = tr069Name.substring(0,(indexOf+1));
                if(!listTr069Names.contains(parameterPath)){
                    listTr069Names.add(parameterPath);
                }
                indexOf = tr069Name.indexOf(".", indexOf + 1);
            }
        }
        Map<String, ParameterDetail> listParameters = findParameterDetailProfile(body, listTr069Names);
        Map<String, Parameter> parameters = parameterDetailService.convertToMapParameter(listParameters);
        Set<Parameter> result = new LinkedHashSet<Parameter>();
        for(Map.Entry<String, Parameter> parameterEntry : parameters.entrySet()){
            result.add(parameterEntry.getValue());
        }
        return result;
    }

    /**
     * parse root infor to get list parameter details
     *
     * @param body
     * @return
     */
    private Map<String, ParameterDetail> findParameterDetailProfile(JsonObject body, List<String> listTr069Names) {
        Map<String, ParameterDetail> mapParam = new HashMap<String, ParameterDetail>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject() && !entry.getKey().contains("summary")) {
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean()) {
                    parameter.defaultValue = "";
                    parameter.rule = "";
                    parameter.path = entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.shortName = entry.getKey();
                    if(listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), entry.getKey() + ".", listTr069Names);
                    }
                }
            }
        }
        return mapParam;
    }

    private void loopFindParameterDetailProfile(Map<String, ParameterDetail> mapParam, JsonObject body, String key, List<String> listTr069Names) {
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                // if param is a object
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean() == true) {
                    parameter.path = key + entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.defaultValue = "";
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    if(listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".",listTr069Names);
                    }
                } else if (body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY).getAsBoolean() == true) {
                    parameter.path = key + entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.defaultValue = "";
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    parameter.instance = true;
                    if(listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".",listTr069Names);
                    }
                } else if (body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString() != null) {
                    if (body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString() != null) {
                        parameter.setValue(body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString());
                        parameter.defaultValue = body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString();
                    }
                    parameter.path = key + entry.getKey();
                    Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(parameter.path);
                    if(tr069Parameter != null) {
                        parameter.rule = tr069Parameter.rule;
                    }
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().contains("xsd") ? body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().replaceAll("xsd:", "") : body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString();
                    parameter.shortName = entry.getKey();
                    parameter.parentObject = key;
                    parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
                    if(listTr069Names.contains(parameter.tr069Name)) {
                        mapParam.put(parameter.path, parameter);
                        loopFindParameterDetailProfile(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".",listTr069Names);
                    }
                }
            }
        }
    }

    public Map<String, Parameter> getParametersOfDevices(String deviceId){
        JsonObject body = getInforDevice(deviceId);
        Map<String, ParameterDetail> listParameters = parseParameterDetail(body);
        Map<String, Parameter> result = parameterDetailService.convertToMapParameter(listParameters);
        return result;
    }

    /**
     * create new deviceTypeVersion and create parameterDetail
     *
     * @param deviceId
     */


    public void addDataModel(String deviceId) {
        try {
            logger.info("Add data model for {}",deviceId);
            JsonObject body = getInforDevice(deviceId);
            DeviceTypeVersion deviceTypeVersion = new DeviceTypeVersion();
            JsonObject inforObject = body.get("_deviceId").getAsJsonObject();
            String modelName = body.get(MODELNAME_KEY) != null ? body.get(MODELNAME_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String firmwareVersion = body.get(SOFTWARE_KEY) != null ? body.get(SOFTWARE_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : "";
            String productClass = inforObject.get(PRODUCTCLASS_KEY) != null ? inforObject.get(PRODUCTCLASS_KEY).getAsString() : renderUnknowName();
            String manufacture = inforObject.get(MANUFACTURE_KEY) != null ? inforObject.get(MANUFACTURE_KEY).getAsString() : renderUnknowName();
            String oui = inforObject.get(OUI_KEY).getAsString() != null ? inforObject.get(OUI_KEY).getAsString() : renderUnknowName();
            DeviceType currenDeviceType = deviceTypeService.findByPk(oui, productClass);
            Map<String, ParameterDetail> mapParam;
            if (currenDeviceType == null) {
                // new deviceType
                DeviceType deviceType = new DeviceType();
                deviceType.manufacturer = manufacture;
                deviceType.oui = oui;
                deviceType.productClass = productClass;
                deviceType.name = productClass + "_" + manufacture + "_" + oui;
                deviceType.modelName = modelName;
                currenDeviceType = deviceTypeService.create(deviceType);
                deviceTypeVersion.deviceTypeId = currenDeviceType.id;
                deviceTypeVersion.firmwareVersion = firmwareVersion;
                deviceTypeVersion.modelName = modelName;
                deviceTypeVersion.productClass = productClass;
                deviceTypeVersion.oui = oui;
                deviceTypeVersion.manufacturer = manufacture;
                mapParam = parseParameterDetail(body);
                // map from ParameterDetail -> Parameter
                deviceTypeVersion.parameters = parameterDetailService.convertToMapParameter(mapParam);
                deviceTypeVersion.id = null;
            } else {
                //device existed
                mapParam = processDataModelForExistedDeviceType(currenDeviceType, body);
                // map from ParameterDetail -> Parameter
                deviceTypeVersion.parameters = parameterDetailService.convertToMapParameter(mapParam);
                deviceTypeVersion.id = null;
                deviceTypeVersion.firmwareVersion = firmwareVersion;
                deviceTypeVersion.deviceTypeId = currenDeviceType.id;
                deviceTypeVersion.modelName = modelName;
                deviceTypeVersion.productClass = productClass;
                deviceTypeVersion.oui = oui;
                deviceTypeVersion.manufacturer = manufacture;
            }
            DeviceTypeVersion deviceTypeVersion1 = deviceTypeVersionService.create(deviceTypeVersion);
            Map<String, Tag> mapDiagnostic = createProfile(deviceTypeVersion1, mapParam);
            if (mapDiagnostic != null) {
                deviceTypeVersion1.diagnostics = mapDiagnostic;
                deviceTypeVersionService.update(deviceTypeVersion1.id, deviceTypeVersion1);
            }
            createDataModel(deviceTypeVersion1, mapParam);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }


    /**
     * create data model for one devicetypeVersion
     *
     * @param deviceTypeVersion
     * @param mapParam
     */
    private void createDataModel(DeviceTypeVersion deviceTypeVersion, Map<String, ParameterDetail> mapParam) {
        // this code takes long time to run -> need optimize
        for (Map.Entry<String, ParameterDetail> entry : mapParam.entrySet()) {
            entry.getValue().deviceTypeVersionId = deviceTypeVersion.id;
            entry.getValue().id = null;
            parameterDetailService.create(entry.getValue());
        }
    }


    /**
     * create profile for one devicetypeVersion
     *
     * @param deviceTypeVersion
     * @param mapParam
     */
    private Map<String, Tag> createProfile(DeviceTypeVersion deviceTypeVersion, Map<String, ParameterDetail> mapParam) {
        // list profile standard tr069
        Map<String, Tag> listProfile = new ConcurrentHashMap<String, Tag>();
        // below tr069 but not below profile of tr069
        Tag profileOther = tagService.generateProfileOther(PROFILE_OTHER, deviceTypeVersion);
        // not below tr069
        Tag profileVendor = tagService.generateProfileOther(PROFILE_VENDOR, deviceTypeVersion);
        // map diagnostics data
        Map<String, Tag> listDiagnostics = new HashMap<String, Tag>();

        for (Map.Entry<String, ParameterDetail> entry : mapParam.entrySet()) {
            // if paramaeter is tr069 standard
            Tr069Parameter tr069Parameter = tr069ParameterService.isTr069ParameterStandard(entry.getValue().tr069Name);
            if (tr069Parameter != null) {
                String profileNames = tr069Parameter.profileNames;

                if (profileNames.isEmpty() || "".equals(profileNames)) {
                    profileOther.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                } else if (!listProfile.isEmpty()) {
                    // if profileNames la mot chuoi cac profile
                    if (profileNames.contains(",")) {
                        List<String> lstConstProfile = Arrays.asList(profileNames.split(","));
                        for (String profile : lstConstProfile) {
                            boolean isBelongToProfile = false;
                            for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
                                if (tmp.getKey().equalsIgnoreCase(profile) && !entry.getValue().dataType.equals("object")) {
                                    // update parameter
                                    isBelongToProfile = true;
                                    tmp.getValue().parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                                    listProfile.put(tmp.getKey(), tmp.getValue());
                                    break;
                                } else {
                                    if (!listProfile.containsKey(profile)) {
                                        tagService.generateProfile(listProfile, profile, deviceTypeVersion);
                                    }
                                }
                            }
                            if (!isBelongToProfile && !entry.getValue().dataType.equals("object")) {
                                profileOther.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                            }
                        }

                    } else {
                        boolean isBelongToProfile = false;
                        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
                            if (tmp.getKey().equalsIgnoreCase(profileNames) && !entry.getValue().dataType.equals("object")) {
                                // update parameter
                                isBelongToProfile = true;
                                tmp.getValue().parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                                listProfile.put(tmp.getKey(), tmp.getValue());
                                break;
                            } else {
                                if (!listProfile.containsKey(profileNames)) {
                                    tagService.generateProfile(listProfile, profileNames, deviceTypeVersion);
                                }
                            }
                        }
                        if (!isBelongToProfile && !entry.getValue().dataType.equals("object")) {
                            profileOther.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                        }
                    }


                } else {
                    tagService.generateProfile(listProfile, profileNames, deviceTypeVersion);
                }
                // create profile
            } else {
                // put to list no standard
                if (!entry.getValue().dataType.equals("object")) {
                    profileVendor.parameters.put(entry.getKey(), parameterDetailService.convertToParameter(entry.getValue()));
                }
            }
        }

        List<Tr069Profile> diagnosticsProfile = tr069ProfileService.getProfileIsDiagnostics();
        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
            boolean blIsDianostics = false;
            for (int dpIndex = 0; dpIndex < diagnosticsProfile.size(); dpIndex++) {
                Tr069Profile profileTemp = diagnosticsProfile.get(dpIndex);
                if (tmp.getKey().equals(profileTemp.name)) {
                    blIsDianostics = true;
                }
            }
            if (blIsDianostics) {
                listDiagnostics.put(tmp.getKey(), tmp.getValue());
            }
        }
        //
        listProfile.put("PROFILE_OTHER", profileOther);
        listProfile.put("PROFILE_VENDOR", profileVendor);
        // create profile standard, profile other and profile vendor
        for (Map.Entry<String, Tag> tmp : listProfile.entrySet()) {
            Tag tag = tagService.create(tmp.getValue());
            for (String key : mapParam.keySet()) {
                if (mapParam.get(key).profile == null) {
                    mapParam.get(key).profile = new HashSet<String>();
                }
                if (tag.parameters.containsKey(mapParam.get(key).path)) {
                    if (mapParam.get(key).profile != null && !mapParam.get(key).profile.contains(tag.id.toString())) {
                        mapParam.get(key).profile.add(tag.id.toString());
                    }
                }
            }
        }
        return listDiagnostics;
    }


    /**
     * render unknow name in case product, manufacture, oui == null
     *
     * @return
     */
    private String renderUnknowName() {
        return "UNKNOWN_" + System.currentTimeMillis();
    }

    /**
     * count duplicate parameter between 2 datamodel
     *
     * @param currentDataModel
     * @param tmpVersionDataModel
     * @return
     */
    private int countDuplicateParameter(Map<String, ParameterDetail> currentDataModel, Map<String, ParameterDetail> tmpVersionDataModel) {
        int count = 0;
        for (String key : currentDataModel.keySet()) {
            if (tmpVersionDataModel.containsKey(key)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * process compare to parse parameter deleted, parameter duplicated, paraemter added
     *
     * @param currentDataModel
     * @param tmpVersionDataModel
     * @return
     */
    private Map<String, ParameterDetail> processCompare(Map<String, ParameterDetail> currentDataModel, Map<String, ParameterDetail> tmpVersionDataModel) {
        for (Map.Entry<String, ParameterDetail> entry : currentDataModel.entrySet()) {
            if (tmpVersionDataModel.containsKey(entry.getKey())) {
                // update default value
                tmpVersionDataModel.get(entry.getKey()).defaultValue = entry.getValue().defaultValue;
                tmpVersionDataModel.put(entry.getKey(), tmpVersionDataModel.get(entry.getKey()));
            } else if (!tmpVersionDataModel.containsKey(entry.getKey())) {
                // new key
                tmpVersionDataModel.put(entry.getKey(), entry.getValue());
            }
            Iterator it = tmpVersionDataModel.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry item = (Map.Entry<String, ParameterDetail>) it.next();
                if (!currentDataModel.containsKey(item.getKey())) {
                    it.remove();
                }
            }
        }
        return tmpVersionDataModel;
    }

    /**
     * @param currenDeviceType
     * @param body
     * @return
     */
    private Map<String, ParameterDetail> processDataModelForExistedDeviceType(DeviceType currenDeviceType, JsonObject body) {
        List<DeviceTypeVersion> listDeviceTypeVersions = deviceTypeVersionService.findByDeviceType(currenDeviceType.id);
        String softwareVersion = body.get(SOFTWARE_KEY) != null ? body.get(SOFTWARE_KEY).getAsJsonObject().get(VALUE_KEY).getAsString() : null;
        String firmwareVersion = getFirmwareVersion(body);
        Map<String, ParameterDetail> currentDataModel = parseParameterDetail(body);
        Map<String, ParameterDetail> mapBeauty = new HashMap<String, ParameterDetail>();
        int maxParameterComplicate = 0;
        for (DeviceTypeVersion deviceTypeVersion : listDeviceTypeVersions) {
            if (deviceTypeVersion.firmwareVersion.equalsIgnoreCase(firmwareVersion) || deviceTypeVersion.firmwareVersion.equalsIgnoreCase(softwareVersion)) {
                // version existed
                throw new DuplicationFirmwareVersionException("firmwareVersion " + deviceTypeVersion.firmwareVersion + " existed!!");
            } else if ((firmwareVersion != null && !deviceTypeVersion.firmwareVersion.equalsIgnoreCase(firmwareVersion))
                    || (softwareVersion != null && !deviceTypeVersion.firmwareVersion.equalsIgnoreCase(softwareVersion))) {
                // choose map which has most parameter duplicated
                Map<String, ParameterDetail> tmpVersionDataModel = parameterDetailService.findByDeviceTypeVersion(deviceTypeVersion.id);
                int max = countDuplicateParameter(currentDataModel, tmpVersionDataModel);
                if (max > maxParameterComplicate) {
                    maxParameterComplicate = max;
                    mapBeauty = tmpVersionDataModel;
                }
            }
        }
        Map<String, ParameterDetail> mapFinal = processCompare(currentDataModel, mapBeauty);
        return mapFinal;
    }

    /**
     * parse root infor to get list parameter details
     *
     * @param body
     * @return
     */
    private Map<String, ParameterDetail> parseParameterDetail(JsonObject body) {
        Map<String, ParameterDetail> mapParam = new HashMap<String, ParameterDetail>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject() && !entry.getKey().contains("summary")) {
                ParameterDetail parameter = new ParameterDetail();
                if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
                    parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
                }
                if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean()) {
                    parameter.defaultValue = "";
                    parameter.rule = "";
                    parameter.path = entry.getKey() + ".";
                    parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
                    parameter.dataType = "object";
                    parameter.shortName = entry.getKey();
                    mapParam.put(entry.getKey() + ".", parameter);
                    loop(mapParam, entry.getValue().getAsJsonObject(), entry.getKey() + ".");
                }
            }
        }
        return mapParam;
    }

    /**
     * loop to go through object
     *
     * @param mapParam
     * @param body
     * @param key
     */
    private void loop(Map<String, ParameterDetail> mapParam, JsonObject body, String key) {
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!ignoredParam.contains(entry.getKey()) && body.get(entry.getKey()).isJsonObject()) {
                // if param is a object
                ParameterDetail parameter = new ParameterDetail();
                processInLoop(entry, body, parameter, key, mapParam);
            }
        }
    }

    /**
     * break code from loop to decreate complication
     *
     * @param entry
     * @param body
     * @param parameter
     * @param key
     * @param mapParam
     */
    private void processInLoop(Map.Entry<String, JsonElement> entry, JsonObject body, ParameterDetail parameter, String key, Map<String, ParameterDetail> mapParam) {
        if (body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY) != null) {
            parameter.access = String.valueOf(body.get(entry.getKey()).getAsJsonObject().get(WRIABLE_KEY).getAsBoolean());
        }
        if (body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(OBJECT_KEY).getAsBoolean() == true) {
            parameter.path = key + entry.getKey() + ".";
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = "object";
            parameter.defaultValue = "";
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            mapParam.put(key + entry.getKey() + ".", parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".");
        } else if (body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(INSTANCE_KEY).getAsBoolean() == true) {
            parameter.path = key + entry.getKey() + ".";
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = "object";
            parameter.defaultValue = "";
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            parameter.instance = true;
            mapParam.put(key + entry.getKey() + ".", parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + entry.getKey() + ".");
        } else if (body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString() != null) {
            if (body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY) != null && body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString() != null) {
                parameter.setValue(body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString());
                parameter.defaultValue = body.get(entry.getKey()).getAsJsonObject().get(VALUE_KEY).getAsString();
            }
            parameter.path = key + entry.getKey();
            Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(parameter.path);
            if(tr069Parameter != null) {
                parameter.rule = tr069Parameter.rule;
            }
            parameter.tr069Name = tr069ParameterService.convertToTr069Param(parameter.path);
            parameter.dataType = body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().contains("xsd") ? body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString().replaceAll("xsd:", "") : body.get(entry.getKey()).getAsJsonObject().get(TYPE_KEY).getAsString();
            parameter.shortName = entry.getKey();
            parameter.parentObject = key;
            parameter.tr069ParentObject = tr069ParameterService.convertToTr069Param(key);
            mapParam.put(key + entry.getKey(), parameter);
            loop(mapParam, entry.getValue().getAsJsonObject(), key + "." + entry.getKey());
        }
    }


    private String getFirmwareVersion(JsonObject jsonObject) {
        if (jsonObject.get("InternetGatewayDevice") != null) {
            return jsonObject.get("InternetGatewayDevice").getAsJsonObject().get("DeviceInfo").getAsJsonObject().get("ModemFirmwareVersion").getAsJsonObject().get(VALUE_KEY).getAsString();
        } else if (jsonObject.get("Device") != null) {
            return jsonObject.get("Device").getAsJsonObject().get("DeviceInfo").getAsJsonObject().get("HardwareVersion").getAsJsonObject().get(VALUE_KEY).getAsString();
        }
        return null;
    }

}
