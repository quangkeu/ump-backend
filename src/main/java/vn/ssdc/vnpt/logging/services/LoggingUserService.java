package vn.ssdc.vnpt.logging.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.logging.endpoints.LoggingDeviceEndpoint;
import vn.ssdc.vnpt.logging.model.ElkLoggingDevice;
import vn.ssdc.vnpt.logging.model.ElkLoggingUser;
import vn.ssdc.vnpt.logging.model.LoggingUser;
import vn.ssdc.vnpt.logging.model.LoggingUserAction;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


@Service
public class LoggingUserService extends SsdcCrudService<Long, LoggingUser> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);
    private static final String INDEX_LOGGING_WEBAPP = "logging_webapp";
    private static final String TYPE_LOGGING_WEBAPP = "logging_webapp";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATETIME_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Autowired
    public ConfigurationService configurationService;

    @Autowired
    public LoggingDeviceService loggingDeviceService;

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    public LoggingUserService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(LoggingUser.class);
    }

    public Page<LoggingUser> getPage(int page, int limit, String name, String actor, String fromDateTime, String toDateTime) {

        String timeExpire = configurationService.get("timeExpire").value;
        if (!("").equals(fromDateTime) && timeExpire != null) {
            String fromDateTimeConvert = StringUtils.convertDateToElk(fromDateTime, DATETIME_FORMAT, DATETIME_ISO_FORMAT);
            String timeExpireConvert = StringUtils.convertDateToElk(loggingDeviceService.convertTimeExpire(timeExpire), DATETIME_FORMAT, DATETIME_ISO_FORMAT);
            if (fromDateTimeConvert.compareTo(timeExpireConvert) < 0) {
                fromDateTime = loggingDeviceService.convertTimeExpire(timeExpire);
            }
        } else {
            fromDateTime = loggingDeviceService.convertTimeExpire(timeExpire);
        }

        String whereExp = "username LIKE ? AND (session LIKE ? OR actions LIKE ?) ";

        if(!"".equals(fromDateTime)) {
            whereExp += " AND created >= " + StringUtils.convertDatetimeToTimestamp(fromDateTime + ".000", "yyyy-MM-dd HH:mm:ss.SSS");
        }
        if(!"".equals(toDateTime)) {
            whereExp += " AND created <= " + StringUtils.convertDatetimeToTimestamp(toDateTime + ".999", "yyyy-MM-dd HH:mm:ss.SSS");
        }
        whereExp += " ORDER BY created DESC";

        Page<LoggingUser> loggingUsers = this.repository.search(whereExp, new PageRequest(page, limit), "%"+actor+"%", "%"+name+"%", "%"+name+"%");

        // Get list cwmps
        for (LoggingUser loggingUser : loggingUsers) {

            // Sort list action by time desc
            List<LoggingUserAction> listActions = new ArrayList<LoggingUserAction>(loggingUser.actions.values());
            sortListActions(listActions);

            // Get list cwmp for action
            loggingUser.actions = new LinkedHashMap<>();
            for (LoggingUserAction loggingUserAction : listActions) {
                if (loggingUserAction.taskId != null) {
                    List<ElkLoggingDevice> elkLoggingDevices = loggingDeviceService.getListElkLoggingDeviceByTaskId(loggingUserAction.taskId);
                    loggingUserAction.loggingDevices = loggingDeviceService.convertToLoggingDevices(elkLoggingDevices);
                }
                loggingUser.actions.put(loggingUser.actions.size(), loggingUserAction);
            }
        }

        return loggingUsers;
    }

    private LoggingUser getBySession(String session) {
        LoggingUser loggingUser = null;

        String where = "session = ?";
        List<LoggingUser> loggingUsers = this.repository.search(where, session);
        if (loggingUsers.size() > 0) {
            loggingUser = loggingUsers.get(0);
        }

        return loggingUser;
    }

    public List<LoggingUser> getUpdateMysql(String fromDateTime, String toDateTime) {
        List<LoggingUser> loggingUsers = new LinkedList<LoggingUser>();
        try {

            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "#USER_LOG"));
            boolQueryBuilder.must(QueryBuilders.matchQuery("_type", TYPE_LOGGING_WEBAPP));

            if (!("").equals(fromDateTime) && !("").equals(toDateTime))
                boolQueryBuilder.should(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime)).lt(parseIsoDate(toDateTime))
                        .includeLower(true).includeUpper(true)).minimumShouldMatch("1");
            if (!("").equals(fromDateTime) && ("").equals(toDateTime))
                boolQueryBuilder.should(QueryBuilders.rangeQuery("@timestamp")
                        .gte(parseIsoDate(fromDateTime))
                        .includeLower(true).includeUpper(true)).minimumShouldMatch("1");
            if (("").equals(fromDateTime) && !("").equals(toDateTime))
                boolQueryBuilder.should(QueryBuilders.rangeQuery("@timestamp")
                        .lt(parseIsoDate(toDateTime))
                        .includeLower(true).includeUpper(true)).minimumShouldMatch("1");

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder).size(999);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_LOGGING_WEBAPP)
                    .addType(TYPE_LOGGING_WEBAPP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingUser> elkLoggingUsers = result.getSourceAsObjectList(ElkLoggingUser.class);
            logger.info("#LOGGING_USER_QUARTZ: " + elkLoggingUsers.size() + " items " + fromDateTime + " - " + toDateTime);
            // Convert data
            for (ElkLoggingUser elkLoggingUser : elkLoggingUsers) {

                String elkMessage = elkLoggingUser.message;

                String time = elkMessage.substring(0, elkMessage.indexOf("INFO")).trim();
                String contentMessage = elkMessage.substring(
                        elkMessage.indexOf("#USER_LOG") + "#USER_LOG".length(),
                        elkMessage.length()).trim();

                // session, username, action, time, device_id, task_id
                String[] messages = contentMessage.split(",", -1);
                if (messages.length == 5) {

                    // Parse data
                    String session = messages[0].trim();
                    String username = messages[1].trim();
                    String action = messages[2].trim();
                    String affected = messages[3].trim();
                    String taskId = messages[4].trim();

                    LoggingUser loggingUser = getBySession(session);
                    if (loggingUser == null) {
                        loggingUser = new LoggingUser();
                        loggingUser.session = session;
                    }

                    if(loggingUser.username == null && !username.isEmpty()) {
                        loggingUser.username = username;
                    }

                    // Check existed action
                    Boolean isExisted = false;
                    for (Integer keyAction : loggingUser.actions.keySet()) {
                        LoggingUserAction loggingUserAction = loggingUser.actions.get(keyAction);
                        if (loggingUserAction.time.equals(time)
                                && loggingUserAction.action.equals(action)) {
                            isExisted = true;
                            break;
                        }
                    }
                    if (!isExisted) {
                        LoggingUserAction loggingUserAction = new LoggingUserAction();
                        loggingUserAction.action = action;
                        loggingUserAction.time = time;
                        loggingUserAction.affected = affected;

                        if (!taskId.isEmpty()) {
                            loggingUserAction.taskId = taskId;
//                            List<ElkLoggingDevice> elkLoggingDevices = loggingDeviceService.getListElkLoggingDeviceByTaskId(taskId);
//                            loggingUserAction.loggingDevices = loggingDeviceService.convertToLoggingDevices(elkLoggingDevices);
                        }

                        loggingUser.actions.put(loggingUser.actions.size(), loggingUserAction);

                        if (loggingUser.id != null) {
                            update(loggingUser.id, loggingUser);
                        } else {
                            create(loggingUser);
                        }
                        logger.info("#LOGGING_USER: "+action+" - "+time+" - ");
                        loggingUsers.add(loggingUser);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("getElkLoggingUser", e);
        }

        return loggingUsers;

    }

    private String parseIsoDate(String date) throws ParseException{
        return StringUtils.convertDateToElk(date, DATETIME_FORMAT, DATETIME_ISO_FORMAT);
    }

    private void sortListActions(List<LoggingUserAction> loggingUserActions) {
        loggingUserActions.sort(new Comparator<LoggingUserAction>() {
            @Override
            public int compare(LoggingUserAction o1, LoggingUserAction o2) {
                return o2.time.compareTo(o1.time);
            }
        });
    }

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    public Boolean removeAllElk(String name, String actor, String fromDateTime, String toDateTime) {

        Boolean result = false;
        try {

            String timeExpire = configurationService.get("timeExpire").value;
            if (!("").equals(fromDateTime) && timeExpire != null) {
                String fromDateTimeConvert = StringUtils.convertDateToElk(fromDateTime, DATETIME_FORMAT, DATETIME_ISO_FORMAT);
                String timeExpireConvert = StringUtils.convertDateToElk(loggingDeviceService.convertTimeExpire(timeExpire), DATETIME_FORMAT, DATETIME_ISO_FORMAT);
                if (fromDateTimeConvert.compareTo(timeExpireConvert) < 0) {
                    fromDateTime = loggingDeviceService.convertTimeExpire(timeExpire);
                }
            } else {
                fromDateTime = loggingDeviceService.convertTimeExpire(timeExpire);
            }

            String whereExp = "username LIKE ? AND (session LIKE ? OR actions LIKE ?) ";

            if(!"".equals(fromDateTime)) {
                whereExp += " AND created >= " + StringUtils.convertDatetimeToTimestamp(fromDateTime + ".000", "yyyy-MM-dd HH:mm:ss.SSS");
            }
            if(!"".equals(toDateTime)) {
                whereExp += " AND created <= " + StringUtils.convertDatetimeToTimestamp(toDateTime + ".999", "yyyy-MM-dd HH:mm:ss.SSS");
            }

            List<LoggingUser> loggingUsers = this.repository.search(whereExp,"%"+actor+"%", "%"+name+"%", "%"+name+"%");

            String query_string = "{\"query_string\":{\"default_field\":\"message\",\"query\":\"%s\"}},";
            StringBuilder must_string = new StringBuilder();
            Set<String> listSessions = new LinkedHashSet<>();
            for (LoggingUser loggingUser : loggingUsers) {
                listSessions.add(loggingUser.session);
                delete(loggingUser.id);
            }
            must_string.append(String.format(query_string, String.join(" ", listSessions)));

            String jsonData = "{" +
                    " \"query\": {" +
                    "  \"bool\": {" +
                    "   \"must\": ["+must_string+"]," +
                    "   \"must_not\": [ ]," +
                    "   \"should\": [ ]" +
                    "  }" +
                    " }" +
                    "}";
            String query = elasticSearchUrl + "/" + INDEX_LOGGING_WEBAPP + "/" + TYPE_LOGGING_WEBAPP+ "/_query";

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


        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
