package vn.ssdc.vnpt.logging.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import vn.ssdc.vnpt.utils.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public class ElkLoggingCwmp {
    @JestId
    public String _id;
    public String message;
    @SerializedName("@timestamp")
    public String timestamp;


    public static final String INDEX_LOGGING_CWMP = "logging_cwmp";
    public static final String TYPE_LOGGING_CWMP = "logging_cwmp";

    public static final String COMPLETED_TASK = "COMPLETED_TASK";
    public static final String FAULT_TASK = "FAULT_TASK";
    public static final String START_TASK = "START_TASK";

    public static final String FORMAT_DATETIME_TO_VIEW = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_DATETIME_STORAGE = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FORMAT_TIMESTAMP_STORAGE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public Long getCreated() {
        Long result = null;
        try {
            String resultStr = message.substring(
                    message.indexOf("[", message.indexOf("[") + 1) + "[".length(),
                    message.indexOf("]", message.indexOf("["))).trim();

            result = StringUtils.convertDatetimeToTimestamp(resultStr, FORMAT_DATETIME_STORAGE);

        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

    public String getDateTime() {
        String result = null;
        try {
            String resultStr = message.substring(
                    message.indexOf("[", message.indexOf("[") + 1) + "[".length(),
                    message.indexOf("]", message.indexOf("["))).trim();

            result = StringUtils.convertDate(resultStr, FORMAT_DATETIME_STORAGE, FORMAT_DATETIME_TO_VIEW);
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

    public String getTaskId() {
        String result = null;
        try {
            result = message.substring(
                    message.indexOf("TASK_ID_") + "TASK_ID_".length(),
                    message.indexOf(" ", message.indexOf("TASK_ID_"))).trim();

            if ("undefined".equals(result)) {
                result = null;
            }
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public Long getPolicyJobId() {
        Long result = null;
        try {

            String resultStr = null;
            if (message.contains(COMPLETED_TASK)) {
                resultStr = message.substring(
                        message.indexOf("POLICY_JOB_") + "POLICY_JOB_".length(),
                        message.length()).trim();
            } else {
                resultStr = message.substring(
                        message.indexOf("POLICY_JOB_") + "POLICY_JOB_".length(),
                        message.indexOf(" ", message.indexOf("POLICY_JOB_"))).trim();
            }

            if (!"undefined".equals(resultStr)) {
                result = Long.valueOf(resultStr);
            } else {
                result = null;
            }
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public String getDeviceId() {
        String result = null;
        try {
            result = message.substring(
                    message.lastIndexOf("_TASK]") + "_TASK]".length(),
                    message.indexOf(":", message.lastIndexOf("_TASK]") + 1)).trim();
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public String getParameter() {
        String result = null;
        try {
            if (message.contains(START_TASK)) {
                result = message.substring(
                        message.indexOf("PARAMETER_") + "PARAMETER_".length(),
                        message.length()).trim();

                if ("undefined".equals(result)) {
                    result = null;
                }
            }

        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public String getErrorCode() {
        String result = null;
        try {
            if (message.contains(FAULT_TASK)) {
                result = message.substring(
                        message.indexOf("ERROR_CODE_") + "ERROR_CODE_".length(),
                        message.indexOf(" ", message.indexOf("ERROR_CODE_"))).trim();

                if ("undefined".equals(result)) {
                    result = null;
                }
            }

        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public String getErrorText() {
        String result = null;
        try {
            if (message.contains(FAULT_TASK)) {
                result = message.substring(
                        message.indexOf("ERROR_TEXT_") + "ERROR_TEXT_".length(),
                        message.length()).trim();

                if ("undefined".equals(result)) {
                    result = null;
                }
            }

        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public String getTaskName() {
        String result = null;
        try {

            String deviceId = getDeviceId();
            result = message.substring(
                    message.indexOf(deviceId) + deviceId.length() + 1,
                    message.indexOf("TASK_ID_", message.indexOf(deviceId))).trim();

        } catch (Exception e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public LoggingPolicy toLoggingPolicy() {
        LoggingPolicy loggingPolicy = new LoggingPolicy();
        loggingPolicy.created = getCreated();
        loggingPolicy.deviceId = getDeviceId();
        loggingPolicy.taskId = getTaskId();
        loggingPolicy.policyJobId = getPolicyJobId();

        return loggingPolicy;
    }

    public LoggingDeviceActivity toDeviceActivity() {
        LoggingDeviceActivity loggingDeviceActivity = new LoggingDeviceActivity();
        loggingDeviceActivity.id = _id;
        loggingDeviceActivity.taskId = getTaskId();
        loggingDeviceActivity.taskName = getTaskName();
        loggingDeviceActivity.parameter = getParameter();
        loggingDeviceActivity.createdTime = getDateTime();

        return loggingDeviceActivity;
    }

}