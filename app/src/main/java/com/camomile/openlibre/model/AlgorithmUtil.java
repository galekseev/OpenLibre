package com.camomile.openlibre.model;

import android.content.res.Resources;

import java.text.DateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.camomile.openlibre.R;

import static java.lang.Math.max;

public class AlgorithmUtil {

    public static final double TREND_UP_DOWN_LIMIT = 15.0; // mg/dl / 10 minutes

    public static final DateFormat mFormatTimeShort = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN);
    //public static final DateFormat mFormatDayTime = DateFormat.getInstance();
    public static final DateFormat mFormatDate = DateFormat.getDateInstance();
    //public static final DateFormat mFormatDateShort = DateFormat.getDateInstance(DateFormat.SHORT);
    public static final DateFormat mFormatDateTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    //public static final DateFormat mFormatDateTimeSec = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);

    public static String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return "";
        }

        char[] buffer = new char[2];
        for (byte b : src) {
            buffer[0] = Character.forDigit((b >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(b & 0x0F, 16);
            builder.append(buffer);
        }

        return builder.toString();
    }

    public static String getDurationBreakdown(Resources resources, long duration)
    {
        duration = max(0, duration);

        long days = TimeUnit.MILLISECONDS.toDays(duration);
        duration -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        duration -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);

        if (days > 1) {
            return days + " " + resources.getString(R.string.days);
        }
        if (days == 1) {
            return "1 " + resources.getString(R.string.day);
        }
        if (hours > 1) {
            return hours + " " + resources.getString(R.string.hours);
        }
        if (hours == 1) {
            return "1 " + resources.getString(R.string.hour);
        }
        if (minutes > 1) {
            return minutes + " " + resources.getString(R.string.minutes);
        }
        if (minutes == 1) {
            return "1 " + resources.getString(R.string.minute);
        }

        return "0 " + resources.getString(R.string.minutes);
    }

}
