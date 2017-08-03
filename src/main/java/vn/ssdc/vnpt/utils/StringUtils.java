package vn.ssdc.vnpt.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StringUtils {

    public static String convertDate(String date, String fromFormat, String toFormat) {
        String result = null;
        try {

            // Declare date
            DateFormat dateFormat = new SimpleDateFormat(fromFormat);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(toFormat);

            // Convert date time
            Date dateDf = dateFormat.parse(date);
            result = simpleDateFormat.format(dateDf);

        } catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    public static String convertDateWithTimeZone(String date, String fromFormat, TimeZone fromTimeZone, String toFormat, TimeZone toTimeZone) {
        String result = null;
        try {

            // Declare date
            DateFormat dateFormat = new SimpleDateFormat(fromFormat);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(toFormat);

            // Set time zone
            dateFormat.setTimeZone(fromTimeZone);
            simpleDateFormat.setTimeZone(toTimeZone);

            // Convert date time
            Date dateDf = dateFormat.parse(date);
            result = simpleDateFormat.format(dateDf);

        } catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }

        return result;
    }

    // Convert from zone +0 to zone default
    public static String convertDateFromElk(String date, String fromFormat, String toFormat) {
        return convertDateWithTimeZone(date, fromFormat, TimeZone.getTimeZone("GMT+0"), toFormat, TimeZone.getDefault());
    }


    // Convert from zone default to zone +0
    public static String convertDateToElk(String date, String fromFormat, String toFormat) {
        return convertDateWithTimeZone(date, fromFormat, TimeZone.getDefault(), toFormat, TimeZone.getTimeZone("GMT+0"));
    }

    public static Long convertDatetimeToTimestamp(String dateTime, String fromFormat) {
        Long result = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(fromFormat);
            Date date = dateFormat.parse(dateTime);
            Timestamp timestamp = new Timestamp(date.getTime());
            result = timestamp.getTime();
        }
        catch (ParseException e) {
            result = null;
            e.printStackTrace();
        }
        return result;
    }

}
