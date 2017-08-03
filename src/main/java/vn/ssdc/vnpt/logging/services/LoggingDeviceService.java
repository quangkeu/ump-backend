package vn.ssdc.vnpt.logging.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.logging.endpoints.LoggingDeviceEndpoint;
import vn.ssdc.vnpt.logging.model.CwmpLoggingDevice;
import vn.ssdc.vnpt.logging.model.ElkLoggingDevice;
import vn.ssdc.vnpt.logging.model.LoggingDevice;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Service
public class LoggingDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);
    private static final String INDEX_LOGGING_DEVICE = "logging_device";
    private static final String TYPE_LOGGING_DEVICE = "logging_device";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATETIME_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String DATETIME_ISO_FILTER_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Autowired
    public ConfigurationService configurationService;

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    public UserService userService;

    @Autowired
    public DeviceGroupService deviceGroupService;

    @Autowired
    public ElkService elkService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    public List<LoggingDevice> getPage(int page, int limit, String name, String actor, String fromDateTime, String toDateTime, String username) {
        List<LoggingDevice> loggingDevices = new LinkedList<LoggingDevice>();
        try {

            SearchResult result = searchElk(page, limit, name, actor, fromDateTime, toDateTime, username);
            List<ElkLoggingDevice> elkLoggingDevices = result.getSourceAsObjectList(ElkLoggingDevice.class);

            // Convert data
            loggingDevices = convertToLoggingDevices(elkLoggingDevices);

        } catch (Exception e) {
            logger.error("getElkLoggingDevice", e);
        }

        return loggingDevices;
    }

    public Long getTotalPages(int page, int limit, String name, String actor, String fromDateTime, String toDateTime, String username) {
        SearchResult result = searchElk(page, limit, name, actor, fromDateTime, toDateTime, username);
        Long totalPage = null;

        try {
            totalPage = (long) Math.floor((int) result.getTotal() / limit) + 1;
        } catch (Exception e) {
            totalPage = Long.valueOf(0);
        }
        return totalPage;
    }

    private SearchResult searchElk(int page, int limit, String name, String actor, String fromDateTime, String toDateTime, String username) {
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "### SESSION ").operator(MatchQueryBuilder.Operator.AND));

            // Add bool query should by device group data
            User user = userService.findByUserName(username);
            for(String deviceGroupId : user.deviceGroupIds){
                DeviceGroup deviceGroup = deviceGroupService.get(Long.parseLong(deviceGroupId));
                boolQueryBuilder.should(getBoolQueryByDeviceGroup(deviceGroup)).minimumShouldMatch("1");
            }

            // Get time expire to set from date time
            String timeExpire = configurationService.get("timeExpire").value;
            if (!("").equals(fromDateTime) && timeExpire != null) {
                if (parseIsoDate(fromDateTime).compareTo(parseIsoDate(convertTimeExpire(timeExpire))) < 0) {
                    fromDateTime = convertTimeExpire(timeExpire);
                }
            } else {
                fromDateTime = convertTimeExpire(timeExpire);
            }

            if (!("").equals(name)) {
                BoolQueryBuilder boolQueryBuilderName = new BoolQueryBuilder();
                boolQueryBuilderName.should(QueryBuilders.matchPhrasePrefixQuery("message", name));
                boolQueryBuilderName.should(QueryBuilders.matchPhrasePrefixQuery("message", "cwmp:"+name)).minimumShouldMatch("1");
                boolQueryBuilder.must(boolQueryBuilderName);
            }
            if (!("").equals(actor)) {
                for (String subActor : actor.split("-")) {
                    if (!subActor.isEmpty()) {
                        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", subActor));
                    }
                }
            }
            if (!("").equals(fromDateTime) && !("").equals(toDateTime))
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)).lt(parseIsoDate(toDateTime))
                        .includeLower(true).includeUpper(true));
            if (!("").equals(fromDateTime) && ("").equals(toDateTime))
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime))
                        .includeLower(true).includeUpper(true));
            if (("").equals(fromDateTime) && !("").equals(toDateTime))
                boolQueryBuilder.must(QueryBuilders.rangeQuery("@timestamp")
                        .lt(parseIsoDate(toDateTime))
                        .includeLower(true).includeUpper(true));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(limit);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_LOGGING_DEVICE)
                    .addType(TYPE_LOGGING_DEVICE)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();

            return elasticSearchClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private BoolQueryBuilder getBoolQueryByDeviceGroup(DeviceGroup deviceGroup) {
        BoolQueryBuilder bqDeviceGroup = QueryBuilders.boolQuery();
        if (deviceGroup.manufacturer != null && !"".equals(deviceGroup.manufacturer) && !"All".equals(deviceGroup.manufacturer)) {
            bqDeviceGroup.must(QueryBuilders.matchPhrasePrefixQuery("message", deviceGroup.manufacturer));
        }
        if (deviceGroup.oui != null && !"".equals(deviceGroup.oui) && !"All".equals(deviceGroup.oui)) {
            bqDeviceGroup.must(QueryBuilders.matchPhrasePrefixQuery("message", deviceGroup.oui));
        }
        if (deviceGroup.firmwareVersion != null && !"".equals(deviceGroup.firmwareVersion) && !"All".equals(deviceGroup.firmwareVersion)) {
            bqDeviceGroup.must(QueryBuilders.matchPhrasePrefixQuery("message", deviceGroup.firmwareVersion));
        }
        if (deviceGroup.label != null && !"".equals(deviceGroup.label)) {
            // input: Hà Nội AND Hà Nam OR Vũng Tàu OR Khánh
            // output: ((("Hà Nội" AND "Hà Nam") OR "Vũng Tàu") OR "Khánh")

            // Get list tag from string label
            String query_string = "";
            String[] subLabelAnds = (" AND " + deviceGroup.label).split(" AND ");
            for (String subLabelAnd : subLabelAnds) {
                String[] subLabelOrs = subLabelAnd.split(" OR ");
                for (int i = 0; i < subLabelOrs.length; i++) {
                    String subLabelOr = subLabelOrs[i].trim();
                    if (!"".equals(subLabelOr)) {
                        // Add first element to andTags & other to orTags
                        if ("".equals(query_string)) {
                            query_string = "\"" + subLabelOr + "\"";
                        } else {
                            if (i == 0) {
                                query_string = "(" + query_string + " AND " + "\"" + subLabelOr + "\"" + ")";
                            } else {
                                query_string = "(" + query_string + " OR " + "\"" + subLabelOr + "\"" + ")";
                            }
                        }
                    }
                }
            }
            bqDeviceGroup.must(QueryBuilders.queryStringQuery(query_string).field("message"));
        }

        return bqDeviceGroup;
    }

    public List<ElkLoggingDevice> getListElkLoggingDeviceByTaskId(String taskId) {

        List<ElkLoggingDevice> elkLoggingDevices = new LinkedList<ElkLoggingDevice>();

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "### SESSION"));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", taskId));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_LOGGING_DEVICE)
                    .addType(TYPE_LOGGING_DEVICE)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            elkLoggingDevices = result.getSourceAsObjectList(ElkLoggingDevice.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return elkLoggingDevices;
    }

    public List<LoggingDevice> convertToLoggingDevices(List<ElkLoggingDevice> elkLoggingDevices) {

        List<LoggingDevice> loggingDevices = new LinkedList<LoggingDevice>();

        for (ElkLoggingDevice elkLoggingDevice : elkLoggingDevices) {
            String elkMessage = elkLoggingDevice.message;

            // Get logging device data
            LoggingDevice loggingDevice = new LoggingDevice();
            loggingDevice.session = getSession(elkMessage);
            if(loggingDevice.session != null) {
                loggingDevice.deviceId = getDeviceId(elkMessage);
                loggingDevice.time = parseViewDate(elkLoggingDevice.timestamp);

                // Get cwmp data
                loggingDevice.cwmps = convertListCwmps(elkMessage);
                if (loggingDevice.cwmps.size() < 4) {
                    loggingDevice.status = "danger";
                } else {
                    for (Integer keyCwmp : loggingDevice.cwmps.keySet()) {
                        CwmpLoggingDevice cwmpLoggingDevice = loggingDevice.cwmps.get(keyCwmp);
                        if ("Fault".equals(cwmpLoggingDevice.cwmp)) {
                            loggingDevice.status = "danger";
                        }
                    }
                }

                // Add to list result
                loggingDevices.add(loggingDevice);
            }
        }

        return loggingDevices;
    }

    public Map<Integer, CwmpLoggingDevice> convertListCwmps(String elkMessage) {

        Map<Integer, CwmpLoggingDevice> cwmpLoggingDeviceMap = new LinkedHashMap<Integer, CwmpLoggingDevice>();

        String[] messages = elkMessage.split("##");
        for (String message : messages) {
            message = message.trim();

            if (message.startsWith("REQUEST")) {
                // Method request
                CwmpLoggingDevice cwmpLoggingDevice = new CwmpLoggingDevice();
                cwmpLoggingDevice.setTypeRequest(getDeviceId(elkMessage));
                cwmpLoggingDevice.cwmp = getCwmp(message);
                cwmpLoggingDevice.eventCode = getValueNode(message, "EventCode");
                cwmpLoggingDevice.time = getDateTimeRequest(message);
                cwmpLoggingDevice.message = message.substring(message.indexOf("{"), message.length());
                cwmpLoggingDeviceMap.put(cwmpLoggingDeviceMap.size(), cwmpLoggingDevice);

            } else if (message.startsWith("RESPONSE")) {
                // Method response
                CwmpLoggingDevice cwmpLoggingDevice = new CwmpLoggingDevice();
                cwmpLoggingDevice.setTypeResponse(getDeviceId(elkMessage));
                cwmpLoggingDevice.cwmp = getCwmp(message);
                cwmpLoggingDevice.eventCode = getValueNode(message, "EventCode");
                cwmpLoggingDevice.time = getDateTimeResponse(message);
                try {
                    cwmpLoggingDevice.message = message.substring(message.indexOf("{"), message.length());
                } catch (Exception e) {
                    // TODO VANLUONG error log elk max 500 line
                    e.printStackTrace();
                }
                cwmpLoggingDeviceMap.put(cwmpLoggingDeviceMap.size(), cwmpLoggingDevice);
            }
        }

        return cwmpLoggingDeviceMap;
    }


    public String convertTimeExpire(String timeExpire) {
        int days = 0;
        String time = "";

        String[] timeArr = timeExpire.split("\\s+");
        for(String t : timeArr) {
            if(t.endsWith("d")) days += Integer.valueOf(t.substring(0, t.length()-1));
            if(t.endsWith("w")) days += Integer.valueOf(t.substring(0, t.length()-1))*7;
            if(t.endsWith("m")) days += Integer.valueOf(t.substring(0, t.length()-1))*30;
            if(t.endsWith("y")) days += Integer.valueOf(t.substring(0, t.length()-1))*365;
        }
        if(days >= 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -days);
            Date date = new Date(cal.getTimeInMillis());
            time = sdf.format(date);
        }
        return time;
    }

    private String getValueNode(String message, String node) {
        String result = null;
        String nodeStart = "<" + node + ">";
        String nodeEnd = "</" + node + ">";
        if(message.contains(nodeStart) && message.contains(nodeEnd)) {
            result = message.substring(
                    message.indexOf(nodeStart) + nodeStart.length(),
                    message.indexOf(nodeEnd)
            ).trim();
        }

        return result;
    }

    private String getCwmp(String message) {
        String result = null;

        if(message.contains("<cwmp:")) {
            result = message.substring(
                    message.lastIndexOf("<cwmp:") + "<cwmp:".length(),
                    message.indexOf(">", message.lastIndexOf("<cwmp:"))
            ).trim();

            if (result.endsWith("/")) {
                result = result.substring(0, result.length()-1);
            }
        } else {
            if (message.startsWith("REQUEST")) {
                result = "EmptyPost";
            } else if (message.startsWith("RESPONSE")) {
                result = "Close";
            }
        }

        return result;
    }

    private String getDeviceId(String message) {
        String result = null;

        String oui = getValueNode(message, "OUI");
        String productClass = getValueNode(message, "ProductClass");
        String serialNumber = getValueNode(message, "SerialNumber");
        result = oui + "-" + productClass + "-" + serialNumber;

        return result;
    }

    private String getSession(String message) {
        if(message.indexOf("### SESSION") == -1){
            return null;
        }
        int indexRequest = message.indexOf("## REQUEST");
        if(indexRequest == -1){
            return null;
        }
        int indexResponse = message.indexOf("## RESPONSE");
        if(indexResponse == -1){
            return null;
        }
        indexRequest = indexRequest > indexResponse ? indexResponse : indexRequest; // Check if session start with ## RESPONSE
        if (indexRequest < 0) {
            return null;
        } else {
            return message.substring(message.indexOf("### SESSION") + "### SESSION".length(), indexRequest).trim();
        }
    }

    private String parseIsoDate(String date){
       return StringUtils.convertDateToElk(date, DATETIME_FORMAT, DATETIME_ISO_FILTER_FORMAT);
    }

    private String parseViewDate(String date) {
        return StringUtils.convertDateFromElk(date, DATETIME_ISO_FORMAT, DATETIME_FORMAT);
    }

    private String getDateTimeRequest(String message) {
        String result = null;
        String fromFormat = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z"; // Thu Jun 15 2017 14:54:20 GMT+0700

        String dateTime = message.substring("REQUEST".length(), message.indexOf("(ICT)")).trim();
        result = StringUtils.convertDate(dateTime, fromFormat, DATETIME_FORMAT);

        return result;
    }

    private String getDateTimeResponse(String message) {
        String result = null;
        String fromFormat = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z"; // Thu Jun 15 2017 14:54:20 GMT+0700

        String dateTime = message.substring("RESPONSE".length(), message.indexOf("(ICT)")).trim();
        result = StringUtils.convertDate(dateTime, fromFormat, DATETIME_FORMAT);

        return result;
    }

    public String exportXML(String session) {
        String strReturn = "ERROR EXPORT ! ";
        try {

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "### SESSION " + session));
            boolQueryBuilder.must(QueryBuilders.matchQuery("_type", "logging_device"));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex(INDEX_LOGGING_DEVICE).addType(TYPE_LOGGING_DEVICE).build();
            SearchResult result = elasticSearchClient.execute(search);
            ElkLoggingDevice elkLoggingDevice  = result.getSourceAsObject(ElkLoggingDevice.class);

            //1st Create XML
            Document document = DocumentHelper.createDocument();
            //Set Root
            Element root = document.addElement(elkLoggingDevice.message);
            // Pretty print the document to System.out
            OutputFormat format = OutputFormat.createPrettyPrint();
            String strTimeCreated = String.valueOf(System.currentTimeMillis());
            File xmlFile = new File(tmpDir + "/session-" + session +"_" + strTimeCreated + ".xml");
            if (xmlFile.createNewFile()) {
                XMLWriter output = new XMLWriter(new FileWriter(xmlFile), format);
                output.write(document);
                output.close();
            }
            strReturn = xmlFile.getAbsolutePath();
        } catch (IOException e) {
            logger.error("{}", e);
            strReturn += e;
        }
        return strReturn;
    }


    public Boolean removeAllElk(String name, String actor, String fromDateTime, String toDateTime) {
        Boolean result = false;
        try {

            String jsonData = convertJsonData(name, actor, fromDateTime, toDateTime);
            String query = elasticSearchUrl + "/" + INDEX_LOGGING_DEVICE + "/" + INDEX_LOGGING_DEVICE + "/_query";

            byte[] postData = jsonData.getBytes(StandardCharsets.UTF_8);
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            while (br.readLine() != null) {
                result = true;
            }
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String convertJsonData(String name, String actor, String fromDateTime, String toDateTime) {

        String range_string = "{\"range\":{\"@timestamp\":{\"gte\":\"%s\",\"lte\":\"%s\"}}},";
        String range_gte_string = "{\"range\":{\"@timestamp\":{\"gte\":\"%s\"}}},";
        String range_lte_string = "{\"range\":{\"@timestamp\":{\"lte\":\"%s\"}}},";
        String query_string = "{\"query_string\":{\"default_field\":\"message\",\"query\":\"%s\"}},";
        StringBuilder must_string = new StringBuilder();

        if (!"".equals(name)) {
            must_string.append(String.format(query_string, name));
        }
        if (!"".equals(actor)) {
            for (String subActor : actor.split("-")) {
                must_string.append(String.format(query_string, subActor));
            }
        }

        String fromDateTimeElk = StringUtils.convertDateToElk(fromDateTime, DATETIME_FORMAT, DATETIME_ISO_FILTER_FORMAT);
        String toDateTimeElk = StringUtils.convertDateToElk(toDateTime, DATETIME_FORMAT, DATETIME_ISO_FILTER_FORMAT);

        if (fromDateTimeElk != null && toDateTimeElk != null) {
            must_string.append(String.format(range_string, fromDateTimeElk, toDateTimeElk));
        } else if (fromDateTimeElk != null) {
            must_string.append(String.format(range_gte_string, fromDateTimeElk));
        } else if (toDateTimeElk != null) {
            must_string.append(String.format(range_lte_string, toDateTimeElk));
        }

        if ((must_string.lastIndexOf(",") + 1) == must_string.length()) {
            must_string = new StringBuilder(must_string.substring(0, must_string.length() - 1));
        }

        String json = "{" +
                " \"query\": {" +
                "  \"bool\": {" +
                "   \"must\": ["+must_string+"]," +
                "   \"must_not\": [ ]," +
                "   \"should\": [ ]" +
                "  }" +
                " }" +
                "}";

        return json;
    }
}
