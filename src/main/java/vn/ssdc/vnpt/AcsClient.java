package vn.ssdc.vnpt;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmPreset;
import vn.ssdc.vnpt.performance.model.PerformancePreset;
import vn.ssdc.vnpt.policy.model.BackupFile;
import vn.ssdc.vnpt.policy.model.PolicyPreset;
import vn.vnpt.ssdc.utils.ObjectUtils;
import vn.vnpt.ssdc.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by vietnq on 10/21/16.
 */
@Component
public class AcsClient {
    private static final int DEFAULT_TIMEOUT = 30000;

    private static final Logger logger = LoggerFactory.getLogger(AcsClient.class);

    private static final String PRESETS = "/presets/";

    private static final String RESPONSE_STATUS_TO_STRING = "Response status: {}";
    private static final String VALUE_KEY = "_value";
    private static final String USER = "admin";

    @Value("${log}")
    private String logDir;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private RestTemplate restTemplate;
    private String acsEndpoint;

    @Value("${backupFileEndpoint}")
    private String backupFileEndpoint;

    public String getAcsEndpoint() {
        return acsEndpoint;
    }

    public String getDirLog() {
        return logDir;
    }

    public String getBackupFileEndpoint() {
        return backupFileEndpoint;
    }

    @Autowired
    public AcsClient(@Value("${acsEndpoint}") String acsEndpoint) {
        this.acsEndpoint = acsEndpoint;
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    }

    /**
     * Search for items in database (e.g. devices, tasks, presets, files)
     *
     * @param collectionName: The data collection to search. Could be one of: tasks, devices, presets, objects.
     * @param queryParams:    Search query. Refer to MongoDB queries for reference.
     * @return Returns a JSON representation of all items in the given collection that match the search criteria.
     */
    public ResponseEntity<String> search(String collectionName, Map<String, String> queryParams) {
        String url = this.acsEndpoint + "/" + collectionName;
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
            logger.debug("Query for devices:{}", queryString);
        }
        return this.restTemplate.getForEntity(url, String.class, queryParams);
    }

    /**
     * Gets device by ID
     *
     * @param deviceId   ID of device in acs
     * @param parameters List of selected parameters, separated by comma
     * @return a ResponseEntity with body is device json string
     */
    public ResponseEntity<String> getDevice(final String deviceId, final String parameters) {
        String url = String.format("%s/devices?query={query}&projection={projection}", acsEndpoint);
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", String.format("{\"_id\":\"%s\"}", deviceId));
        params.put("projection", parameters);
        return this.restTemplate.getForEntity(url, String.class, params);
    }

    public String lastObjectInstance(String deviceId, String objectName) {
        String instance = "";
        ResponseEntity<String> response = getDevice(deviceId, objectName);
        if (!ObjectUtils.empty(response.getBody())) {
            JsonArray array = new Gson().fromJson(response.getBody(), JsonArray.class);
            JsonObject object = array.get(0).getAsJsonObject();
            String tmpObject = objectName.endsWith(".") ? objectName.substring(0, objectName.length() - 1) : objectName;
            String[] paths = tmpObject.split("\\.");
            for (String path : paths) {
                object = object.getAsJsonObject(path);
            }
            Iterator<Map.Entry<String, JsonElement>> iterator = object.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonElement> entry = iterator.next();
                try {
                    entry.getValue().getAsJsonObject();
                    instance = entry.getKey();
                } catch (IllegalStateException e) {
                    logger.error("{}", e);
                }
            }
        }
        return instance;
    }

    /**
     * Reboot policy a device
     *
     * @param deviceId ID of device
     * @param now      Boolean If true, reboot now, if false reboot at next inform
     * @return ResponseEntity: status 200 if task succeeds, 202 if task is queued
     */
    public ResponseEntity<String> reboot(String deviceId, Boolean now, Long policyJobId) {
        return createRebootTask(deviceId, null, now, policyJobId);
    }

    /**
     * Reboot a device
     *
     * @param deviceId ID of device
     * @param now      Boolean If true, reboot now, if false reboot at next inform
     * @return ResponseEntity: status 200 if task succeeds, 202 if task is queued
     */
    public ResponseEntity<String> reboot(String deviceId, Boolean now) {
        return createRebootTask(deviceId, null, now, null);
    }

    public ResponseEntity<String> createRebootTask(String deviceId, String commandKey, Boolean now, Long policyJobId){
        try {
            logger.info("Reboot device: {}, now: {}", deviceId, now);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("name", "reboot");
            if(commandKey != null){
                datas.put("commandKey", commandKey);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("reboot ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Factory reset policy a device
     *
     * @param deviceId ID of device
     * @param now      Boolean If true, reboot now, if false reboot at next inform
     * @return ResponseEntity: status 200 if task succeeds, 202 if task is queued
     */
    public ResponseEntity<String> factoryReset(String deviceId, Boolean now, Long policyJobId) {
        return createFactoryResetTask(deviceId, now, policyJobId);
    }

    /**
     * Factory reset a device
     *
     * @param deviceId ID of device
     * @param now      Boolean If true, reboot now, if false reboot at next inform
     * @return ResponseEntity: status 200 if task succeeds, 202 if task is queued
     */
    public ResponseEntity<String> factoryReset(String deviceId, Boolean now) {
        return createFactoryResetTask(deviceId, now, null);
    }

    public ResponseEntity<String> createFactoryResetTask(String deviceId, Boolean now, Long policyJobId) {
        try {
            logger.info("Factory reset device: {}, now: {}", deviceId, now);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("name", "factoryReset");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("factoryReset ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> addObject(String deviceId, String objectName, Map<String, String> parameterValues, Boolean now, Long policyJobId) {
        return createAddObjectTask(deviceId, objectName, parameterValues, now, policyJobId);
    }

    public ResponseEntity<String> addObject(String deviceId, String objectName, Map<String, String> parameterValues, Boolean now) {
        return createAddObjectTask(deviceId, objectName, parameterValues, now, null);
    }

    public ResponseEntity<String> createAddObjectTask(String deviceId, String objectName, Map<String, String> parameterValues, Boolean now, Long policyJobId) {
        String body = "";
        try {
            String url = taskApiUrl(deviceId, now);
            JsonObject datas = new JsonObject();
            if(policyJobId != null){
                datas.addProperty("policyJobId", policyJobId.toString());
            }
            datas.addProperty("name", "addObject");
            if (objectName != null && objectName.length() > 0 && objectName.charAt(objectName.length()-1)=='.') {
                objectName = objectName.substring(0, objectName.length()-1);
            }
            datas.addProperty("objectName", objectName);
            JsonArray array = new JsonArray();
            for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
                JsonArray param = new JsonArray();
                param.add(entry.getKey());
                param.add(entry.getValue());
                array.add(param);
            }
            datas.add("parameterValues", array);
            body = new Gson().toJson(datas);
            logger.info("Adding object {} for device #{}", body, deviceId);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, body, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("addObject ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> deleteObject(String deviceId, String objectName, Boolean now, Long policyJobId) {
        return createDeleteObjectTask(deviceId, objectName, now, policyJobId);
    }

    public ResponseEntity<String> deleteObject(String deviceId, String objectName, Boolean now) {
        return createDeleteObjectTask(deviceId, objectName, now, null);
    }

    public ResponseEntity<String> createDeleteObjectTask(String deviceId, String objectName, Boolean now, Long policyJobId) {
        try {
            logger.info("Delete object {} for device #{}", objectName, deviceId);
            String url = taskApiUrl(deviceId, now);
            JsonObject datas = new JsonObject();
            if(policyJobId != null){
                datas.addProperty("policyJobId", policyJobId.toString());
            }
            datas.addProperty("name", "deleteObject");
            if (objectName != null && objectName.length() > 0 && objectName.charAt(objectName.length()-1)=='.') {
                objectName = objectName.substring(0, objectName.length()-1);
            }
            datas.addProperty("objectName", objectName);
            String body = new Gson().toJson(datas);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, body, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("deleteObject ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> refreshObject(String deviceId, final String objectName, Boolean now, Long policyJobId) {
        return createRefreshObjectTask(deviceId, objectName, now, policyJobId);
    }

    public ResponseEntity<String> refreshObject(String deviceId, final String objectName, Boolean now) {
        return createRefreshObjectTask(deviceId, objectName, now, null);
    }

    public ResponseEntity<String> createRefreshObjectTask(String deviceId, final String objectName, Boolean now, Long policyJobId) {
        try {
            logger.info("Refreshing object {} for device #{}", objectName, deviceId);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            datas.put("name", "refreshObject");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("objectName", objectName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            logger.info("Response body: {}", responseEntity.getBody());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("refreshObject ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> downloadFile(String deviceId, String fileId, String fileName, Boolean now) {
        return createDownloadFileTask(deviceId, fileId, fileName, now, null);
    }

    public ResponseEntity<String> downloadFile(String deviceId, String fileId, String fileName, Boolean now, Long policyJobId) {
        return createDownloadFileTask(deviceId, fileId, fileName, now, policyJobId);
    }

    public ResponseEntity<String> createDownloadFileTask(String deviceId, String fileId, String fileName, Boolean now, Long policyJobId) {
        try {
            logger.info("Download file {} for device #{}", fileId, deviceId);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            datas.put("name", "download");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("file", fileId);
            datas.put("filename", fileName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            logger.info("Response body: {}", responseEntity.getBody());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("downloadFile ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> createDownloadUrlFileTask(String deviceId, String fileType, String urlFile,
                                                            String username, String password,
                                                            String successUrl, String failureUrl, String commandKey, Integer fileSize,
                                                            String targetFileName, Integer delaySeconds, Boolean status,
                                                            String startTime, String completeTime,
                                                            Boolean now) {
        try {
            logger.info("Download url {} for device #{}", urlFile, deviceId);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            datas.put("name", "download");
            datas.put("fileType", fileType);
            datas.put("url", urlFile);
            datas.put("username", username);
            datas.put("password", password);
            datas.put("successUrl", successUrl);
            datas.put("failureUrl", failureUrl);
            datas.put("commandKey", commandKey);
            datas.put("fileSize", fileSize.toString());
            datas.put("targetFileName", targetFileName);
            datas.put("delaySeconds", delaySeconds.toString());
            datas.put("status", status.toString());
            datas.put("startTime", startTime);
            datas.put("completeTime", completeTime);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            logger.info("Response body: {}", responseEntity.getBody());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("downloadFile ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> uploadFile(String deviceId, String fileType, Boolean now) {
        ResponseEntity<String> response = getDevice(deviceId,"summary.softwareVersion");
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        if(array.size()>0) {
            JsonObject object = array.get(0).getAsJsonObject();
            String softwareVersion = object.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString();
            String uploadFileUrl = String.format("%s/backup-files/%s-%s-%s-%s", backupFileEndpoint, fileType.substring(0, 1), System.currentTimeMillis(), softwareVersion, deviceId);
            return createUploadFileTask(deviceId, fileType, uploadFileUrl, null, null, null, null, now, null);
        }
        else{
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<String> uploadFile(String deviceId, String fileType, Boolean now, Long policyJobId) {
        ResponseEntity<String> response = getDevice(deviceId,"summary.softwareVersion");
        String body = (String) response.getBody();
        JsonArray array = new Gson().fromJson(body, JsonArray.class);
        if(array.size()>0) {
            JsonObject object = array.get(0).getAsJsonObject();
            String softwareVersion = object.get("summary.softwareVersion").getAsJsonObject().get("_value").getAsString();
            String uploadFileUrl = String.format("%s/backup-files/%s-%s-%s-%s", backupFileEndpoint, fileType.substring(0, 1), System.currentTimeMillis(), softwareVersion, deviceId);
            return createUploadFileTask(deviceId, fileType, uploadFileUrl, null, null, null, null, now, policyJobId);
        }
        else{
            return new ResponseEntity<String>("", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<String> createUploadFileTask(String deviceId, String fileType, String uploadFileUrl, String username, String password, Integer delaySeconds, String commandKey, Boolean now, Long policyJobId) {
        try {
            logger.info("Upload file for device #{}", deviceId);
            String url = taskApiUrl(deviceId, now);
            Map<String, String> datas = new HashMap<String, String>();
            datas.put("name", "upload");
            datas.put("fileType", fileType);
            datas.put("url", uploadFileUrl);
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            if(username != null){
                datas.put("username", username);
            }
            if(password != null){
                datas.put("password", password);
            }
            if(delaySeconds != null){
                datas.put("delaySeconds", delaySeconds.toString());
            }
            if(commandKey != null){
                datas.put("commandKey", commandKey);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
            logger.info("Response body: {}", responseEntity.getBody());
            return responseEntity;
        } catch (Exception e) {
            logger.error("uploadFile ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> getParameterNames(String deviceId, String parameterPath, Boolean nextLevel, Boolean now) {
        return createGetParameterNamesTask(deviceId, parameterPath, nextLevel, now, null);
    }

    public ResponseEntity<String> getParameterNames(String deviceId, String parameterPath, Boolean nextLevel, Boolean now, Long policyJobId) {
        return createGetParameterNamesTask(deviceId, parameterPath, nextLevel, now, policyJobId);
    }

    public ResponseEntity<String> createGetParameterNamesTask(String deviceId, String parameterPath, Boolean nextLevel, Boolean now, Long policyJobId) {
        try {
            String url = taskApiUrl(deviceId, now);
            Map<String, Object> datas = new HashMap<String, Object>();
            datas.put("name", "getParameterNames");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("parameterPath", parameterPath);
            datas.put("nextLevel", nextLevel);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            logger.info("Executed task get parameter names, status: {}", responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("getParameterNames ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> getParameterValues(String deviceId, final List<String> parameters, Boolean now) {
        return createGetParameterValuesTask(deviceId, parameters, now, null);
    }

    public ResponseEntity<String> getParameterValues(String deviceId, final List<String> parameters, Boolean now, Long policyJobId) {
        return createGetParameterValuesTask(deviceId, parameters, now, policyJobId);
    }

    public ResponseEntity<String> createGetParameterValuesTask(String deviceId, final List<String> parameters, Boolean now, Long policyJobId) {
        try {
            String url = taskApiUrl(deviceId, now);
            Map<String, Object> datas = new HashMap<String, Object>();
            datas.put("name", "getParameterValues");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("parameterNames", parameters);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            logger.info("Executed task get parameter values, status: {}", responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("getParameterValues ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> setParameterValues(String deviceId, Map<String, Object> parameterValues, Boolean now) {
        return createSetParameterValuesTask(deviceId, parameterValues, now, null);
    }

    public ResponseEntity<String> setParameterValues(String deviceId, Map<String, Object> parameterValues, Boolean now, Long policyJobId) {
        return createSetParameterValuesTask(deviceId, parameterValues, now, policyJobId);
    }

    public ResponseEntity<String> createSetParameterValuesTask(String deviceId, Map<String, Object> parameterValues, Boolean now, Long policyJobId) {
        List<List<Object>> params = new ArrayList<List<Object>>();
        List<String> listPath = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
            listPath.add(entry.getKey());
        }
        try {
            logger.info("Setting values for parameters: {}, now: {}", new Gson().toJson(parameterValues), now);
            String url = taskApiUrl(deviceId, now);
            Map<String, Object> datas = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                List<Object> objects = new ArrayList<Object>();
                objects.add(entry.getKey());
                objects.add(entry.getValue());
                params.add(objects);
            }
            datas.put("name", "setParameterValues");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("parameterValues", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info("Executed task set parameters value, status: {}", responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("setParameterValues ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private String taskApiUrl(String deviceId, Boolean now) {
        String url = String.format("%s/devices/%s/tasks?timeout=%d", acsEndpoint, deviceId, DEFAULT_TIMEOUT);
        if (now) {
            url += "&connection_request";
        }
        return url;
    }

    /**
     * Append a task to queue for a given device.
     *
     * @param deviceId    The ID of the device
     * @param object      Collection of data to send
     * @param queryParams params of header
     * @return ResponseEntity represent for returned result
     */
    public ResponseEntity<String> postTasksToDevice(String deviceId, Object object, Map<String, String> queryParams) throws RestClientException {
        StringBuilder url = new StringBuilder("/devices/" + deviceId + "/tasks");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url.append("?" + queryString);
        }
        return restTemplate.postForEntity(acsEndpoint + url.toString(), object, String.class, queryParams);
    }

    /**
     * Retry a faulty task at the next inform.
     *
     * @param taskId : The ID of the task as returned by 'GET /tasks' request
     * @return Response entity
     */
    public ResponseEntity<String> retryTask(String taskId) {
        String fullPath = acsEndpoint + "/tasks/{taskId}/retry";
        Map<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("taskId", taskId);
        Map<String, String> datas = new HashMap<String, String>();
        return restTemplate.postForEntity(fullPath, datas, String.class, uriVariables);
    }

    public ResponseEntity<String> pingDevice(String ipDevice){
        String fullPath = acsEndpoint + "/ping/{ipDevice}";
        Map<String, String> result = new HashMap<String, String>();
        result.put("ipDevice",ipDevice);
        Map<String, String> datas = new HashMap<String, String>();
        return restTemplate.postForEntity(fullPath, datas, String.class, result);
    }

    /**
     * Delete specific taskId
     *
     * @param taskId
     * @throws RestClientException if delete is not success
     */
    public void deleteTask(String taskId) {
        String fullPath = acsEndpoint + "/tasks/{taskId}";
        Map<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("taskId", taskId);
        restTemplate.delete(fullPath, uriVariables);
    }

    public void createPerformancePreset(PerformancePreset performancePreset, String presetName) {
        String url = acsEndpoint + PRESETS + presetName;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(performancePreset, headers);
            restTemplate.put(url, entity);
        } catch (RestClientException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public void createAlarmPreset(AlarmPreset alarmPreset, String presetName) {
        String url = acsEndpoint + PRESETS + presetName;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(alarmPreset, headers);
            restTemplate.put(url, entity);
        } catch (RestClientException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public void createPolicyPreset(PolicyPreset policyPreset, String presetName) {
        String url = acsEndpoint + PRESETS + presetName;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(policyPreset, headers);
            restTemplate.put(url, entity);
        } catch (RestClientException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }


    public void deletePolicyPreset(String presetName) {
        String url = acsEndpoint + PRESETS + presetName;
        try {
            restTemplate.delete(url, new HashMap<String, String>());
        } catch (RestClientException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public void deletePerformancePreset(String presetName) {
        String url = acsEndpoint + PRESETS + presetName;
        try {
            restTemplate.delete(url, new HashMap<String, String>());
        } catch (RestClientException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public ResponseEntity<String> checkExist(final String deviceId, Map<String, String> params) {
        String url = String.format("%s/devices?query={query}", acsEndpoint);
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(url, String.class, params);
        return  responseEntity;
    }

    public ResponseEntity<String> addLabel(String deviceId, String label) {
        String url = acsEndpoint + "/devices/" + deviceId + "/tags/" + label;
        ResponseEntity<String> stringResponseEntity = this.restTemplate.postForEntity(url, "", String.class);
        return stringResponseEntity;
    }


    public boolean deleteLabel(String deviceId, String label) {
        String url = acsEndpoint + "/devices/" + deviceId + "/tags/" + label;
        try {
            this.restTemplate.delete(url);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean deleteDevice(String deviceId) {
        String url = acsEndpoint + "/devices/" + deviceId;
        try {
            this.restTemplate.delete(url);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public ResponseEntity<String> searchFile(String collectionName, Map<String, String> queryParams) {
        String url = this.acsEndpoint + "/" + collectionName;
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
            logger.debug("Query for devices:{}", queryString);
        }
        return this.restTemplate.getForEntity(url, String.class, queryParams);
    }

    public ResponseEntity<String> searchPerformancePreset(String performancePreset) {
        String url = this.acsEndpoint + "/presets";
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("query", String.format("{\"_id\":\"%s\"}", performancePreset));
        queryParams.put("projection", "_id");

        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
            logger.debug("Query for devices:{}", queryString);
        }
        return this.restTemplate.getForEntity(url, String.class, queryParams);
    }


    public BackupFile searchBackupFile(String deviceId) {
        String url = backupFileEndpoint + "/files";
        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("query", "{\"metadata.fileType\":\"3 Vendor Configuration File\", \"filename\": { \"$regex\": \""+deviceId+"\" }}");
        queryParams.put("limit", "1");
        queryParams.put("sort", "{\"uploadDate\":-1}");
        String queryString = StringUtils.queryStringFromMap(queryParams);
        if (!ObjectUtils.empty(queryString)) {
            url += "?" + queryString;
            logger.debug("Query for devices:{}", queryString);
        }
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(url, String.class, queryParams);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        if(jsonArray.size()>0) {
            BackupFile backupFile = new BackupFile();
            JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
            backupFile.id = jsonObject.get("_id").getAsString();
            backupFile.filename = jsonObject.get("filename").getAsString();
            return backupFile;
        }
        else{
            return null;
        }
    }

    public String deleteDevice(String table, String paramID, String paramName) {
        String result = "";
        String url = acsEndpoint + "/" + table + "/" + paramName;
        try {
            this.restTemplate.delete(url);
            result = "";
        } catch (Exception e) {
            result = paramName;
        }
        return result;
    }

    public ResponseEntity<String> refreshAll(String deviceId, Boolean now) {
        String url = taskApiUrl(deviceId, now);
        Map<String, String> datas = new HashMap<String, String>();
        datas.put("name", "refreshAll");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
        ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
        logger.info(RESPONSE_STATUS_TO_STRING, responseEntity.getStatusCodeValue());
        logger.info("Response body: {}", responseEntity.getBody());
        return responseEntity;
    }

    public ResponseEntity<String> updateDiagnosticResult(String deviceId, Long diagnosticTaskId, final List<String> parameters, Boolean now) {
        String url = taskApiUrl(deviceId, now);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "updateDiagnosticResult");
        map.put("parameterNames", parameters);
        map.put("diagnosticTaskId", diagnosticTaskId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<Object>(map, headers);
        ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
        return responseEntity;
    }

    public Map<String, String> getDeviceParameters(String deviceId, List<String> listPath) {
        Map<String, String> uriVariable = new HashMap<String, String>();
        uriVariable.put("query", "{\"_id\":\"" + deviceId + "\"}");
        uriVariable.put("projection", org.apache.commons.lang3.StringUtils.join(listPath, ","));
        ResponseEntity<String> responseEntity = this.search("devices", uriVariable);
        JsonArray jsonArray = new Gson().fromJson(responseEntity.getBody(), JsonArray.class);
        JsonObject deviceObject = jsonArray.get(0).getAsJsonObject();
        HashMap<String, String> result = new HashMap<String, String>();
        for (String path : listPath) {
            result.put(path, getParamValue(deviceObject, path));
        }
        return result;
    }


    private static String getParamValue(JsonObject deviceObject, String paramName) {
        String value = "";

        if (paramName.startsWith("summary")) {
            try {
                deviceObject = deviceObject.get(paramName).getAsJsonObject();
            } catch (Exception e) {
                logger.error("{}", e);
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

        return value;
    }

    public ResponseEntity<String> recheckStatus(String deviceId) {
        String url = String.format("%s/connection-request/%s", acsEndpoint, deviceId);
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(url, String.class);
        return  responseEntity;
    }

    public ResponseEntity<String> createDiagnostic(String deviceId, Map<String, Object> parameterValues, Boolean now) {
        return createDiagnosticTask(deviceId, parameterValues, now, null);
    }

    public ResponseEntity<String> createDiagnostic(String deviceId, Map<String, Object> parameterValues, Boolean now, Long policyJobId) {
        return createDiagnosticTask(deviceId, parameterValues, now, policyJobId);
    }

    public ResponseEntity<String> createDiagnosticTask(String deviceId, Map<String, Object> parameterValues, Boolean now, Long policyJobId) {
        List<List<Object>> params = new ArrayList<List<Object>>();
        List<String> listPath = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
            listPath.add(entry.getKey());
        }
        try {
            logger.info("Setting values for parameters: {}, now: {}", new Gson().toJson(parameterValues), now);
            String url = taskApiUrl(deviceId, now);
            Map<String, Object> datas = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                List<Object> objects = new ArrayList<Object>();
                objects.add(entry.getKey());
                objects.add(entry.getValue());
                params.add(objects);
            }
            datas.put("name", "createDiagnostic");
            if(policyJobId != null){
                datas.put("policyJobId", policyJobId.toString());
            }
            datas.put("parameterValues", params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<Object>(datas, headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(url, entity, String.class);
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseEntity.getBody());
            logger.info("Executed task set parameters value, status: {}", responseEntity.getStatusCodeValue());
            return responseEntity;
        } catch (RestClientException e) {
            logger.error("createDiagnostic ", e);
            return new ResponseEntity<String>("", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
