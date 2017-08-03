package vn.ssdc.vnpt.logging.services;

import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.logging.endpoints.LoggingDeviceEndpoint;
import vn.ssdc.vnpt.logging.model.ElkLoggingCwmp;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;
import vn.ssdc.vnpt.logging.model.LoggingPolicy;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Service
public class LoggingPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    @Autowired
    JestClient elasticSearchClient;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    public List<PolicyTask> getPage(int page, int limit, Long policyJobId) {
        List<PolicyTask> policyTasks = new LinkedList<PolicyTask>();
        try {

            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", ElkLoggingCwmp.START_TASK));

            if (policyJobId > 0) {
                boolQueryBuilder.must(QueryBuilders.matchQuery("message", "POLICY_JOB_" + policyJobId.toString()));
            }

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(20);
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

            for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
                LoggingPolicy loggingPolicy = elkLoggingCwmp.toLoggingPolicy();

                // Search completed task or fault task
                if (loggingPolicy.taskId != null) {
                    ElkLoggingCwmp elkLoggingCwmpCompleted = getLogByPolicyId(policyJobId, loggingPolicy.taskId, ElkLoggingCwmp.COMPLETED_TASK);
                    if (elkLoggingCwmpCompleted != null) {
                        elkLoggingCwmp = elkLoggingCwmpCompleted;
                        loggingPolicy.completed = elkLoggingCwmp.getCreated();
                        loggingPolicy.status = 1;
                    } else {
                        ElkLoggingCwmp elkLoggingCwmpFault = getLogByPolicyId(policyJobId, loggingPolicy.taskId, ElkLoggingCwmp.FAULT_TASK);
                        elkLoggingCwmp = elkLoggingCwmpFault != null ? elkLoggingCwmpFault : elkLoggingCwmp;
                        loggingPolicy.completed = elkLoggingCwmp.getCreated();
                        loggingPolicy.status = 2;
                        loggingPolicy.errorCode = elkLoggingCwmp.getErrorCode();
                        loggingPolicy.errorText = elkLoggingCwmp.getErrorText();
                    }
                }

                policyTasks.add(loggingPolicy.toPolicyTask());
            }

        } catch (Exception e) {
            logger.error("getPageLoggingPolicy", e);
        }

        return policyTasks;
    }

    public List<LoggingDeviceActivity> getPageDeviceActivity(int page, int limit, String deviceId, String fromDateTime, String toDateTime) {
        List<LoggingDeviceActivity> loggingDeviceActivities = new LinkedList<LoggingDeviceActivity>();
        try {

            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", ElkLoggingCwmp.START_TASK));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("message", deviceId));

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
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder).from((page - 1) * limit).size(limit);
            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);

            // Get list logging device activity
            for (ElkLoggingCwmp elkLoggingCwmp : elkLoggingCwmps) {
                LoggingDeviceActivity loggingDeviceActivity = elkLoggingCwmp.toDeviceActivity();

                // Search completed task and fault task
                if (deviceId != null && !"".equals(deviceId)) {
                    ElkLoggingCwmp elkLoggingCwmpCompleted = getLogByDeviceId(deviceId, loggingDeviceActivity.taskId, ElkLoggingCwmp.COMPLETED_TASK);
                    if (elkLoggingCwmpCompleted != null) {
                        loggingDeviceActivity.completedTime = elkLoggingCwmpCompleted.getDateTime();
                    }

                    ElkLoggingCwmp elkLoggingCwmpFault = getLogByDeviceId(deviceId, loggingDeviceActivity.taskId, ElkLoggingCwmp.FAULT_TASK);
                    if (elkLoggingCwmpFault != null) {
                        loggingDeviceActivity.errorCode = elkLoggingCwmpFault.getErrorCode();
                        loggingDeviceActivity.errorText = elkLoggingCwmpFault.getErrorText();
                    }
                }

                loggingDeviceActivities.add(loggingDeviceActivity);
            }

        } catch (Exception e) {
            logger.error("getPageLoggingDeviceActivity", e);
        }

        return loggingDeviceActivities;
    }

    private ElkLoggingCwmp getLogByPolicyId(Long policyJobId, String taskId, String taskType) {
        ElkLoggingCwmp elkLoggingCwmp = null;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", taskType));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "TASK_ID_" + taskId));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "POLICY_JOB_" + policyJobId));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (elkLoggingCwmps.size() > 0) {
                elkLoggingCwmp = elkLoggingCwmps.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return elkLoggingCwmp;
    }

    private ElkLoggingCwmp getLogByDeviceId(String deviceId, String taskId, String taskType) {
        ElkLoggingCwmp elkLoggingCwmp = null;

        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", taskType));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "TASK_ID_" + taskId));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", deviceId));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            List<ElkLoggingCwmp> elkLoggingCwmps = result.getSourceAsObjectList(ElkLoggingCwmp.class);
            if (elkLoggingCwmps.size() > 0) {
                elkLoggingCwmp = elkLoggingCwmps.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return elkLoggingCwmp;
    }

    public Boolean removeAllElk() {
        Boolean result = false;
        try {
            URL url = new URL(elasticSearchUrl + "/" + ElkLoggingCwmp.INDEX_LOGGING_CWMP);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

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
            logger.error("removeAllLog", e);
        }

        return result;
    }

    public Map<String, Long> getSummary(Long policyJobId) {

        Map<String, Long> result = new LinkedHashMap<>();

        Long totalElements = getTotalElement(policyJobId);
        Long totalCompleted = getTotalCompleted(policyJobId);
        Long totalError = getTotalFault(policyJobId);
        Long totalInprocess = totalElements - totalCompleted - totalError;

        result.put("totalElements", totalElements);
        result.put("totalInprocess", totalInprocess);
        result.put("totalCompleted", totalCompleted);
        result.put("totalError", totalError);

        return result;
    }

    public Map<String, Long> getSummaryDeviceActivity(String deviceId, String fromDateTime, String toDateTime) {

        Map<String, Long> result = new LinkedHashMap<>();
        Long totalElements = getTotalElementDeviceActivity(deviceId, fromDateTime, toDateTime);
        result.put("totalElements", totalElements);

        return result;
    }

    private Long getTotalElementDeviceActivity(String deviceId, String fromDateTime, String toDateTime) {
        Long result = null;
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", ElkLoggingCwmp.START_TASK));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", deviceId));

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
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult resultElk = elasticSearchClient.execute(search);
            result = Long.valueOf(resultElk.getTotal());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Long getTotalByType(Long policyJobId, String taskType) {
        Long result = null;
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", taskType));
            boolQueryBuilder.must(QueryBuilders.matchQuery("message", "POLICY_JOB_" + policyJobId));

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .addType(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build();
            SearchResult resultElk = elasticSearchClient.execute(search);
            result = Long.valueOf(resultElk.getTotal());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Long getTotalElement(Long policyJobId) {
        return getTotalByType(policyJobId, "START_TASK");
    }

    private Long getTotalCompleted(Long policyJobId) {
        return getTotalByType(policyJobId, "COMPLETED_TASK");
    }

    private Long getTotalFault(Long policyJobId) {
        return getTotalByType(policyJobId, "FAULT_TASK");
    }

    private String parseIsoDate(String date){
        return StringUtils.convertDateToElk(date, ElkLoggingCwmp.FORMAT_DATETIME_TO_VIEW, ElkLoggingCwmp.FORMAT_TIMESTAMP_STORAGE);
    }

    public Boolean removeById(String id) {
        Boolean result = true;
        try {
            elasticSearchClient.execute(new Delete.Builder(id)
                    .index(ElkLoggingCwmp.INDEX_LOGGING_CWMP)
                    .type(ElkLoggingCwmp.TYPE_LOGGING_CWMP)
                    .build());

        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }

        return result;
    }
}
