package vn.ssdc.vnpt.devices.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Admin on 3/14/2017.
 */
public class DateUtils {
    public static final String FORM_ISODATE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String FORM_DATE_READABLE = "yyyy-MM-dd HH:mm:ss";
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static Date convertIsoDateToDate(String isoDate) {
        DateFormat df1 = new SimpleDateFormat(FORM_ISODATE);
        DateFormat df2 = new SimpleDateFormat(FORM_DATE_READABLE);
        Date result = null;
        try {
            result = df1.parse(isoDate);
            String dateFormated = df2.format(result);
            return df2.parse(dateFormated);

        } catch (ParseException e) {
            logger.error("{}", e);
        }
        return result;
    }

    public static String convertIsoDateToString(String isoDate) {
        DateFormat df1 = new SimpleDateFormat(FORM_ISODATE);
        DateFormat df2 = new SimpleDateFormat(FORM_DATE_READABLE);
        Date result = null;
        try {
            result = df1.parse(isoDate);
            return df2.format(result);
        } catch (ParseException e) {
            logger.error("{}", e);
        }
        return "";
    }
}
