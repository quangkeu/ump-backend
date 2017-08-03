package vn.ssdc.vnpt.alarm.services;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.model.*;
import vn.ssdc.vnpt.common.services.EmailTemplateService;
import vn.ssdc.vnpt.common.services.MailService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.logging.model.ElkLoggingDevice;
import vn.ssdc.vnpt.logging.model.LoggingDevice;
import vn.ssdc.vnpt.logging.services.LoggingDeviceService;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by Lamborgini on 5/24/2017.
 */
@Service
public class AlarmService extends SsdcCrudService<Long, Alarm> {
    private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);

    public static String INDEX = "logging_cwmp";
    public static String TYPE = "logging_cwmp";

    public static String INDEX_REQUEST_FAIL = "logging_device";
    public static String TYPE_REQUEST_FAIL = "logging_device";

    public static String INDEX_PERFORMANCE = "ump_performance";
    public static String TYPE_PERFORMANCE = "ump_performance";

    private static final String SOFTWARE_VERSION_KEY = "summary.softwareVersion";
    private static final String DEVICEID_KEY = "_deviceId";

    @Autowired
    private AlarmTypeService alarmTypeService;

    @Autowired
    private AlarmDetailsService alarmDetailsService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Autowired
    public MailService mailService;

    @Autowired
    public AcsClient acsClient;

    @Autowired
    public LoggingDeviceService loggingDeviceService;

    @Autowired
    public UserService userService;

    @Autowired
    public EmailTemplateService emailTemplateService;

    @Autowired
    public AlarmService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Alarm.class);
    }

    public void monitoringCWMPLog(String fromDate, String toDate) throws IOException {
        JestClient jestClient = elasticSearchClient();
        List<ElasticsearchLogObject> logList;

        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "VALUE_CHANGE"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        TimeZone tz = TimeZone.getTimeZone("GMT+0");
        sdf.setTimeZone(tz);

        for (ElasticsearchLogObject elo : logList) {
            int beginString = elo.message.lastIndexOf("[VALUE_CHANGE]");
            String strMessageHandle = elo.message.substring(beginString + "[VALUE_CHANGE] ".length(), elo.message.length());
            int spilitLocation = strMessageHandle.indexOf(":");
            String strDeviceID = strMessageHandle.substring(0, spilitLocation);
            String strJsonValue = strMessageHandle.substring(spilitLocation + 1, strMessageHandle.length());

            Gson gson = new Gson();
            JsonElement element = gson.fromJson(strJsonValue, JsonElement.class);
            JsonArray jsonArray = element.getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement entry = jsonArray.get(i);
                JsonArray entryArray = entry.getAsJsonArray();
                String strTags = getTagByDevice(strDeviceID);

                String source = jsonBuilder()
                        .startObject()
                        .field("deviceId", strDeviceID)
                        .field("parameterName", entryArray.get(0).getAsString())
                        .field("value", entryArray.get(1).getAsString())
                        .field("@timestamp", sdf.format(new Date()))
                        .field("tags", strTags)
                        .endObject().string();
                Index index = new Index.Builder(source).index(INDEX_PERFORMANCE).type(TYPE_PERFORMANCE).build();
                jestClient.execute(index);
            }
        }

    }


    public void processAlarm(String fromDate, String toDate) throws ParseException {
        //STEP 1 : GET ALL ALARMTYPE NOTIFY = 1
        List<AlarmType> lstAlarmType = alarmTypeService.findByNotify();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTime = String.valueOf(sdf.parse(toDate).getTime());
        String startTime = String.valueOf(sdf.parse(fromDate).getTime());


        //STEP 2 : GET ALL ALARMDETAIL BY ALARM TYPE AND TIME
        for (int index = 0; index < lstAlarmType.size(); index++) {
            AlarmType alarmType = lstAlarmType.get(index);
            Long alarmTypeId = alarmType.id;
            List<AlarmDetails> lstAlarmDetail = alarmDetailsService.findByMonitoring(alarmTypeId, startTime, endTime);
            //STEP 3 : IF SIZE > 0 INSERT TO ALARM
            if (lstAlarmDetail.size() > 0) {
                for (int index_2 = 0; index_2 < lstAlarmDetail.size(); index_2++) {
                    AlarmDetails alarmDetails = lstAlarmDetail.get(index_2);
                    ////CREATE ALARM
                    Alarm alarm = new Alarm();
                    alarm.deviceId = alarmDetails.device_id;
                    alarm.alarmTypeId = alarmDetails.alarm_type_id;
                    alarm.alarmTypeName = alarmDetails.alarm_type_name;
//                    alarm.deviceGroupId = ?
//                    alarm.deviceGroupName = ?
                    alarm.deviceGroups = alarmType.deviceGroups;
                    alarm.raised = alarmDetails.created;
                    alarm.status = "Active";
                    alarm.severity = alarmType.severity;
                    alarm.alarmName = alarmType.name;
                    this.create(alarm);
                }
                if (alarmType.notifyAggregated.equals("EMAIL")) {
                    //CHECK SEND MAIL
                    List<Alarm> lstAlarmActive = getAlarmActiveById(alarmType.id);
                    // List<Alarm> lstAlarmActive = getAlarmActiveById(22l);
                    if (lstAlarmActive.size() >= alarmType.aggregatedVolume) {
                        //GET ALL USER ALARM
                        Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                        Set<String> listMailSend = new HashSet<>();

                        for (DeviceGroup temp
                                : deviceGroups) {
                            String strSearchID = temp.id.toString();
                            List<User> listUserByDeviceGroupId = userService.getListUserByDeviceGroupId(strSearchID);
                            for (User tempUser : listUserByDeviceGroupId)
                                listMailSend.add(tempUser.email);
                        }
                        //SEND MAIL ALL USER
                        String mailContent = String.format(emailTemplateService.get("alarm.notify_ver2").value,
                                alarmType.severity,
                                alarmType.type,
                                alarmType.name);
                        System.out.println("LIST MAIL TO SEND SIZE : " + listMailSend.size() + " ALARM TYPE : " + alarmType.name);
                        for (String strEMail : listMailSend) {
                            logger.info("SENDING MAIL TO : " + strEMail);
                            mailService.sendMail(strEMail, "ALARM DEVICE OVER AGGREGATED VOLUME", mailContent, null, null);
                        }
                    }
                }
            }
        }
    }

//    String fromDate = "2017-06-05 00:00:00";
//    String toDate = "2017-06-05 23:59:59";
//    String INDEX = "ump";
//    String TYPE = "alarmdata";


    public void processingAlarmDetail(String fromDate, String toDate) throws IOException {
        //Get Alarm Type
        List<AlarmType> lAlarmType = alarmTypeService.findByMonitoring();
        for (int index = 0; index < lAlarmType.size(); index++) {
            AlarmType alarmType = lAlarmType.get(index);
            createQueryAlarmType(alarmType, fromDate, toDate);
        }
    }


    public void createQueryAlarmType(AlarmType alarmType, String fromDate, String toDate) throws IOException {
        JestClient jestClient = elasticSearchClient();

        //STEP 1 : Check Alarm Type
        if (alarmType.type.equalsIgnoreCase("REQUEST_FAIL")) {
            handleRequestFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("CONFIGURATION_FAIL")) {
            handleConfigurationFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("UPDATE_FIRMWARE_FAIL")) {
            handleUpdateFirmWareFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("REBOOT_FAIL")) {
            handleRebootFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("FACTORY_RESET_FAIL")) {
            handleFactoryResetFail(alarmType, fromDate, toDate, jestClient);
        } else if (alarmType.type.equalsIgnoreCase("PARAMETER_VALUE")) {
            handleParameterValue(alarmType, fromDate, toDate, jestClient);
        } else {
            logger.info("UNKNOW ALARM TYPE : " + alarmType.type);
        }
    }

    public void handleParameterValue(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        Map<String, String> mapCompare = alarmType.parameterValues;
        List<String> lstKey = new ArrayList<>();
        for (Map.Entry<String, String> entry : mapCompare.entrySet()) {
            lstKey.add(entry.getKey());
        }

        //STEP 1 : Get Data From ELK
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            for (String strCompare : lstKey) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("parameterName", strCompare)).minimumShouldMatch("1");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX_PERFORMANCE);
        builder.addType(TYPE_PERFORMANCE);


        SearchResult result = jestClient.execute(builder.build());
        List<UmpPerformance> lstUmpPerformance = result.getSourceAsObjectList(UmpPerformance.class);

        //
        for (Map.Entry<String, String> entry : mapCompare.entrySet()) {
            String strParam = entry.getKey();
            for (UmpPerformance umpPerformance : lstUmpPerformance) {
                if (strParam.equalsIgnoreCase(umpPerformance.parameterName)) {
                    String valueCompare = entry.getValue();
                    String valueDevice = umpPerformance.value;
                    if (checkCompare(valueCompare, valueDevice)) {
                        createAlarmDetail(alarmType, umpPerformance.deviceId);
                    }
                }
            }
        }
        //
    }

    public boolean checkCompare(String valueCompare, String valueDevice) {
        boolean blReturn = false;

        try {
            if (valueCompare.equalsIgnoreCase("true") || valueCompare.equalsIgnoreCase("false")) {
                // handle with string
                if (valueCompare.equalsIgnoreCase(valueDevice)) {
                    blReturn = true;
                }
            } else {
                // handle with number

//                >;>=;<;<=
//                >50
//                50<=70
//                60
                if (!valueCompare.contains(",")) {
                    blReturn = processCompare(valueCompare, valueDevice);
                } else {
                    String[] agru = valueCompare.split(",");
                    for (String strAgru : agru) {
                        blReturn = processCompare(strAgru, valueDevice);
                        if (blReturn) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Method checkCompare :" + ex.toString());
        }
        return blReturn;
    }

    public boolean processCompare(String valueCompare, String valueDevice) {
        boolean blReturn = false;
        try {
            //
            long lValueDevice = Long.parseLong(valueDevice);
            //
            int indexBegin = 0;
            int indexEnd = 0;

            String a = "";
            String b = "";
            String operator = "";

            //get index param
            if (valueCompare.contains(">") && !valueCompare.contains("=")) {
                indexBegin = valueCompare.indexOf(">");
                indexEnd = valueCompare.indexOf(">") + 1;
                operator = ">";
            }
            if (valueCompare.contains("<") && !valueCompare.contains("=")) {
                indexBegin = valueCompare.indexOf("<");
                indexEnd = valueCompare.indexOf("<") + 1;
                operator = "<";
            }
            if (valueCompare.contains(">=")) {
                indexBegin = valueCompare.indexOf(">=");
                indexEnd = valueCompare.indexOf(">=") + 2;
                operator = ">=";
            }
            if (valueCompare.contains("<=")) {
                indexBegin = valueCompare.indexOf("<=");
                indexEnd = valueCompare.indexOf("<=") + 2;
                operator = "<=";
            }
            //
            a = valueCompare.substring(0, indexBegin);
            b = valueCompare.substring(indexEnd, valueCompare.length());
            boolean haveBegin = false;

            if (a.equals("")) {
                long valueBegin = 0;
                long valueEnd = Long.parseLong(b);
                blReturn = compare(valueBegin, valueEnd, operator, lValueDevice, haveBegin);
            } else {
                haveBegin = true;
                long valueBegin = Long.parseLong(a);
                long valueEnd = Long.parseLong(b);
                blReturn = compare(valueBegin, valueEnd, operator, lValueDevice, haveBegin);
            }

        } catch (NumberFormatException nfe) {
            if (valueCompare.equals(valueDevice)) {
                return true;
            } else {
                return false;
            }
        }
        return blReturn;
    }

    public boolean compare(long valueBegin, long valueEnd, String operator, long deviceValue, boolean haveBegin) {
        boolean blReturn = false;
//        >;>=;<;<=

        if (haveBegin) {
            if (operator.equals(">")) {
                if (valueBegin > deviceValue && deviceValue > valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<")) {
                if (valueBegin < deviceValue && deviceValue < valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals(">=")) {
                if (valueBegin >= deviceValue && deviceValue >= valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<=")) {
                if (valueBegin <= deviceValue && deviceValue <= valueEnd) {
                    blReturn = true;
                }
            }
        } else {
            if (operator.equals(">")) {
                if (deviceValue > valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<")) {
                if (deviceValue < valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals(">=")) {
                if (deviceValue >= valueEnd) {
                    blReturn = true;
                }
            }
            if (operator.equals("<=")) {
                if (deviceValue <= valueEnd) {
                    blReturn = true;
                }
            }
        }

        return blReturn;
    }

    public void handleRequestFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.mustNot(QueryBuilders.matchPhrasePrefixQuery("message", "HTTP EMPTY POST"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX_REQUEST_FAIL);
        builder.addType(TYPE_REQUEST_FAIL);

        SearchResult result = jestClient.execute(builder.build());
        List<ElkLoggingDevice> elkLoggingDevices = result.getSourceAsObjectList(ElkLoggingDevice.class);

        // Convert data
        List<LoggingDevice> loggingDevices = new LinkedList<LoggingDevice>();
        loggingDevices = loggingDeviceService.convertToLoggingDevices(elkLoggingDevices);

        //For each device
        for (LoggingDevice loggingDevice : loggingDevices) {
            Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
            //STEP 3 : If Device In Device Type
            if (checkDeviceInAlarmType(loggingDevice.deviceId, deviceGroups)) {
                //STEP 4 : Create Alarm Detail
                createAlarmDetail(alarmType, loggingDevice.deviceId);
            }
        }
    }

    public void handleUpdateFirmWareFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "download"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                if (elo.message.contains("[FAULT_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID);
                    }
                } else if (elo.message.contains("[COMPLETED_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    //Convert timestamp to fromdate
                    //Create new from date after get the COMPLETED_TASK
                    String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                    //
                    try {
                        boolQueryBuilder = new BoolQueryBuilder();
                        boolQueryBuilder.must(QueryBuilders
                                .rangeQuery("@timestamp")
                                .gte(parseIsoDate(strFromDateReboot))
                                .lt(parseIsoDate(toDate))
                                .includeLower(true)
                                .includeUpper(true));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strDeviceID + ":"));
                        boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "Inform (0 BOOTSTRAP)"));
                        boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "Inform (7 TRANSFER COMPLETE)"));

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(boolQueryBuilder).size(9999);
                    builder = new Search.Builder(searchSourceBuilder.toString());
                    builder.addIndex(INDEX);
                    builder.addType(TYPE);

                    result = jestClient.execute(builder.build());
                    List<ElasticsearchLogObject> lstResult = result.getSourceAsObjectList(ElasticsearchLogObject.class);
                    // If Don't have inform 1 BOOT create Alarm
                    //TODO : CHECK HAVE 0 BOOTSTRAP AND 7 TRANSFER COMPLETE

                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    public void handleConfigurationFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "setParameterValues"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "addObject"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);
        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                //STEP 3 : If Device In Device Type
                if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                    //STEP 4 : Create Alarm Detail
                    createAlarmDetail(alarmType, strDeviceID);
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    public void handleFactoryResetFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "factoryReset"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Reboot Fail
                // Case 1: [FAULT_TASK] & reboot
                if (elo.message.contains("[FAULT_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID);
                    }
                }
                // Case 2:
                //        o	[COMPLETED_TASK] & reboot
                //        o	Inform 1 BOOT
                else if (elo.message.contains("[COMPLETED_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    //Convert timestamp to fromdate
                    //Create new from date after get the COMPLETED_TASK
                    String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                    //
                    try {
                        boolQueryBuilder = new BoolQueryBuilder();
                        boolQueryBuilder.must(QueryBuilders
                                .rangeQuery("@timestamp")
                                .gte(parseIsoDate(strFromDateReboot))
                                .lt(parseIsoDate(toDate))
                                .includeLower(true)
                                .includeUpper(true));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strDeviceID + ":"));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "Inform"));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "0 BOOTSTRAP"));

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(boolQueryBuilder).size(9999);
                    builder = new Search.Builder(searchSourceBuilder.toString());
                    builder.addIndex(INDEX);
                    builder.addType(TYPE);

                    result = jestClient.execute(builder.build());
                    List<ElasticsearchLogObject> lstResult = result.getSourceAsObjectList(ElasticsearchLogObject.class);
                    // If Don't have inform 1 BOOT create Alarm
                    if (lstResult.size() == 0) {
                        createAlarmDetail(alarmType, strDeviceID);
                    }
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }


    public void handleRebootFail(AlarmType alarmType, String fromDate, String toDate, JestClient jestClient) throws IOException {
        //STEP 1 : Get Data From ELK
        List<ElasticsearchLogObject> logList;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders
                .boolQuery();

        try {
            boolQueryBuilder.must(QueryBuilders
                    .rangeQuery("@timestamp")
                    .gte(parseIsoDate(fromDate))
                    .lt(parseIsoDate(toDate))
                    .includeLower(true)
                    .includeUpper(true));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[FAULT_TASK]"));
            boolQueryBuilder.should(QueryBuilders.matchPhrasePrefixQuery("message", "[COMPLETED_TASK]"));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "reboot"));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder).size(9999);
        Search.Builder builder = new Search.Builder(searchSourceBuilder.toString());
        builder.addIndex(INDEX);
        builder.addType(TYPE);

        SearchResult result = jestClient.execute(builder.build());
        logList = result.getSourceAsObjectList(ElasticsearchLogObject.class);

        //STEP 2 : For Each AlarmLogList Get DeviceID
        for (int i = 0; i < logList.size(); i++) {
            ElasticsearchLogObject elo = logList.get(i);
            try {
                // Handle Reboot Fail
                // Case 1: [FAULT_TASK] & reboot
                if (elo.message.contains("[FAULT_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[FAULT_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[FAULT_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    Set<DeviceGroup> deviceGroups = alarmType.deviceGroups;
                    //STEP 3 : If Device In Device Type
                    if (checkDeviceInAlarmType(strDeviceID, deviceGroups)) {
                        //STEP 4 : Create Alarm Detail
                        createAlarmDetail(alarmType, strDeviceID);
                    }
                }
                // Case 2:
                //        o	[COMPLETED_TASK] & reboot
                //        o	Inform 1 BOOT
                else if (elo.message.contains("[COMPLETED_TASK]")) {
                    int beginString = elo.message.lastIndexOf("[COMPLETED_TASK]");
                    String strMessageHandle = elo.message.substring(beginString + "[COMPLETED_TASK]".length(), elo.message.length());
                    String strDeviceID = removeLastChar(strMessageHandle.split(" ")[1]);
                    //Convert timestamp to fromdate
                    //Create new from date after get the COMPLETED_TASK
                    String strFromDateReboot = convertTimeStampToDate(elo.timestamp);
                    //
                    try {
                        boolQueryBuilder = new BoolQueryBuilder();
                        boolQueryBuilder.must(QueryBuilders
                                .rangeQuery("@timestamp")
                                .gte(parseIsoDate(strFromDateReboot))
                                .lt(parseIsoDate(toDate))
                                .includeLower(true)
                                .includeUpper(true));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", strDeviceID + ":"));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "Inform"));
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", "1 BOOT"));

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(boolQueryBuilder).size(9999);
                    builder = new Search.Builder(searchSourceBuilder.toString());
                    builder.addIndex(INDEX);
                    builder.addType(TYPE);

                    result = jestClient.execute(builder.build());
                    List<ElasticsearchLogObject> lstResult = result.getSourceAsObjectList(ElasticsearchLogObject.class);
                    // If Don't have inform 1 BOOT create Alarm
                    if (lstResult.size() == 0) {
                        createAlarmDetail(alarmType, strDeviceID);
                    }
                }
            } catch (Exception ex) {
                logger.error("Unknow Exception " + ex);
            }
        }
    }

    private String convertTimeStampToDate(String strTimeStamp) {
        strTimeStamp = strTimeStamp.replace("Z", " ");
        strTimeStamp = strTimeStamp.replace("T", " ");
        strTimeStamp = strTimeStamp.substring(0, strTimeStamp.indexOf("."));
        return strTimeStamp;
    }

    private String getTagByDevice(String deviceID) {
        String strReturn = "";
        String paramters = "_tags";
        ResponseEntity response = acsClient.getDevice(deviceID, paramters);
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        if (array.size() > 0) {
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get("_tags") != null) {
                strReturn = object.get("_tags").toString();
            }
        }
        return strReturn;
    }

    private Boolean checkDeviceInAlarmType(String deviceID, Set<DeviceGroup> deviceGroups) {
        String paramters = "summary.softwareVersion,_deviceId._OUI,_deviceId._Manufacturer,_deviceId._ProductClass";
        ResponseEntity response = acsClient.getDevice(deviceID, paramters);
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        if (array.size() > 0) {
            String strFirmware = "";
            JsonObject object = array.get(0).getAsJsonObject();
            if (object.get(SOFTWARE_VERSION_KEY) != null && object.get(SOFTWARE_VERSION_KEY).getAsJsonObject().get("_value") != null) {
                strFirmware = object.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString();
            }
            String manufacturer = object.get(DEVICEID_KEY).getAsJsonObject().get("_Manufacturer").getAsString();
            String productClass = object.get(DEVICEID_KEY).getAsJsonObject().get("_ProductClass").getAsString();
            String oui = object.get(DEVICEID_KEY).getAsJsonObject().get("_OUI").getAsString();

            for (DeviceGroup temp : deviceGroups) {
                if (temp.firmwareVersion == null || temp.firmwareVersion.equalsIgnoreCase(strFirmware))
                    if (temp.oui == null || temp.oui.equalsIgnoreCase(oui))
                        if (temp.productClass == null || temp.productClass.equalsIgnoreCase(productClass))
                            if (temp.manufacturer.equals("All") || temp.manufacturer.equalsIgnoreCase(manufacturer))
                                return true;
                break;
            }
        }
        return false;
    }

    private static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    public void createAlarmDetail(AlarmType alarmType, String strDeviceId) {
        AlarmDetails alarmDetailsModel = new AlarmDetails();
        alarmDetailsModel.alarm_type_id = alarmType.id;
        alarmDetailsModel.alarm_type = alarmType.type;
        alarmDetailsModel.alarm_type_name = alarmType.name;
        alarmDetailsModel.device_id = strDeviceId;
        alarmDetailsModel.deviceGroups = alarmType.deviceGroups;
        //
        alarmDetailsService.create(alarmDetailsModel);
    }

    public List<Alarm> searchAlarm(String limit, String indexPage, String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.search(whereExp, new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), new Sort(Sort.Direction.DESC, "id"))).getContent();
        } else {
            alarmList = this.repository.findAll(new PageRequest(Integer.parseInt(indexPage), Integer.parseInt(limit), new Sort(Sort.Direction.DESC, "id"))).getContent();
        }

        return alarmList;
    }

    public int countAlarm(String whereExp) {
        int count = 0;
        if (whereExp.isEmpty()) {
            count = (int) this.repository.count();
        } else {
            count = (int) this.repository.count(whereExp);
        }
        return count;
    }

    public List<Alarm> getAlarmNameByAlarmType(String alarmType) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!alarmType.isEmpty()) {
            alarmList = this.repository.search("alarm_type_name like ?", "%" + alarmType + "%");
        }
        return alarmList;
    }

    public List<Alarm> getAlarmActiveById(Long alarmTypeId) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        alarmList = this.repository.search("alarm_type_id =  ?  and status = 'Active' ", alarmTypeId);

        return alarmList;
    }

    public List<Alarm> viewGraphSeverityAlarm(String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.searchWithGroupBy("severity", whereExp);
        } else {
            alarmList = this.repository.searchWithGroupBy("severity");
        }

        return alarmList;
    }

    public List<Alarm> viewGraphNumberOfAlarmType(String whereExp) {
        List<Alarm> alarmList = new ArrayList<Alarm>();
        if (!whereExp.isEmpty()) {
            alarmList = this.repository.searchWithGroupBy("alarm_name", whereExp);
        } else {
            alarmList = this.repository.searchWithGroupBy("alarm_name");
        }

        return alarmList;
    }

    public JestClient elasticSearchClient() {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)

                .build());

        return jestClientFactory.getObject();
    }

    public static String parseIsoDate(String date) throws ParseException {
        return StringUtils.convertDateToElk(date, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }
}
