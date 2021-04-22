package com.camomile.openlibre.model;


import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.camomile.openlibre.model.AlgorithmUtil.mFormatDateTime;
import static com.camomile.openlibre.model.GlucoseData.formatValue;

public class BloodGlucoseData extends RealmObject implements Comparable<BloodGlucoseData> {
    public static final String ID = "id";
    static final String GLUCOSE_LEVEL = "glucoseLevel";
    public static final String DATE = "date";
    public static final String TIMEZONE_OFFSET_IN_MINUTES = "timezoneOffsetInMinutes";

    @PrimaryKey
    private String id;
    private float glucoseLevel = -1; // in mg/dl
    private long date;
    private int timezoneOffsetInMinutes;

    public BloodGlucoseData() {}
    public BloodGlucoseData(long date, float glucoseLevel) {
        this.date = date;
        timezoneOffsetInMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60;
        this.glucoseLevel = glucoseLevel;
        id = String.format(Locale.US, "blood_%s", mFormatDateTime.format(new Date(date)));
    }

    public float glucose() {
        return glucoseLevel;
    }

    public String glucoseString() {
        return formatValue(glucose());
    }

    @Override
    public int compareTo(@NonNull BloodGlucoseData another) {
        return (int) (date - another.date);
    }

    public long getDate() {
        return date;
    }

    public int getTimezoneOffsetInMinutes() {
        return timezoneOffsetInMinutes;
    }
}
