package com.camomile.openlibre.model;

import android.support.annotation.NonNull;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.camomile.openlibre.OpenLibre.GLUCOSE_UNIT_IS_MMOL;

public class GlucoseData extends RealmObject implements Comparable<GlucoseData> {
    public static final String ID = "id";
    public static final String SENSOR = "sensor";
    public static final String AGE_IN_SENSOR_MINUTES = "ageInSensorMinutes";
    public static final String GLUCOSE_LEVEL_RAW = "glucoseLevelRaw";
    public static final String IS_TREND_DATA = "isTrendData";
    public static final String DATE = "date";
    public static final String TIMEZONE_OFFSET_IN_MINUTES = "timezoneOffsetInMinutes";

    @PrimaryKey
    private String id;
    private SensorData sensor;
    private boolean isTrendData = false;
    private int ageInSensorMinutes = -1;
    private int glucoseLevelRaw = -1; // in mg/l = 0.1 mg/dl
    private long date;
    private int timezoneOffsetInMinutes;

    public GlucoseData() {}
    public GlucoseData(SensorData sensor, int ageInSensorMinutes, int timezoneOffsetInMinutes, int glucoseLevelRaw, boolean isTrendData, long date) {
        this.sensor = sensor;
        this.ageInSensorMinutes = ageInSensorMinutes;
        this.timezoneOffsetInMinutes = timezoneOffsetInMinutes;
        this.glucoseLevelRaw = glucoseLevelRaw;
        this.isTrendData = isTrendData;
        this.date = date;
        id = generateId(sensor, ageInSensorMinutes, isTrendData, glucoseLevelRaw);
    }
    public GlucoseData(SensorData sensor, int ageInSensorMinutes, int timezoneOffsetInMinutes, int glucoseLevelRaw, boolean isTrendData) {
        this(sensor, ageInSensorMinutes, timezoneOffsetInMinutes, glucoseLevelRaw, isTrendData, sensor.getStartDate() + TimeUnit.MINUTES.toMillis(ageInSensorMinutes));
    }

    public static String generateId(SensorData sensor, int ageInSensorMinutes, boolean isTrendData, int glucoseLevelRaw) {
        if (isTrendData) {
            // a trend data value for a specific time is not fixed in its value, but can change on the next reading
            // so the trend id also includes the glucose value itself, so the previous reading's data are not overwritten
            return String.format(Locale.US, "trend_%s_%05d_%03d", sensor.getId(), ageInSensorMinutes, glucoseLevelRaw);
        } else {
            return String.format(Locale.US, "history_%s_%05d", sensor.getId(), ageInSensorMinutes);
        }
    }

    public static float convertGlucoseMMOLToMGDL(float mmol) {
        return mmol * 18f;
    }

    public static float convertGlucoseMGDLToMMOL(float mgdl) {
        return mgdl / 18f;
    }

    private static float convertGlucoseRawToMGDL(float raw) {
        return raw / 10f;
    }

    private static float convertGlucoseRawToMMOL(float raw) {
        return convertGlucoseMGDLToMMOL(raw / 10f);
    }

    public static float convertGlucoseMGDLToDisplayUnit(float mgdl) {
        return GLUCOSE_UNIT_IS_MMOL ? convertGlucoseMGDLToMMOL(mgdl) : mgdl;
    }

    public static float convertGlucoseRawToDisplayUnit(float raw) {
        return GLUCOSE_UNIT_IS_MMOL ? convertGlucoseRawToMMOL(raw) : convertGlucoseRawToMGDL(raw);
    }

    public static String getDisplayUnit() {
        return GLUCOSE_UNIT_IS_MMOL ? "mmol/l" : "mg/dl";
    }

    public float glucose(boolean as_mmol) {
        return as_mmol ? convertGlucoseRawToMMOL(glucoseLevelRaw) : convertGlucoseRawToMGDL(glucoseLevelRaw);
    }

    public float glucose() {
        return convertGlucoseRawToDisplayUnit(glucoseLevelRaw);
    }

    public static String formatValue(float value) {
        return GLUCOSE_UNIT_IS_MMOL ?
                new DecimalFormat("##.0").format(value) :
                new DecimalFormat("###").format(value);
    }
    public String glucoseString() {
        return formatValue(glucose());
    }

    @Override
    public int compareTo(@NonNull GlucoseData another) {
        return (int) (getDate() - another.getDate());
    }

    public SensorData getSensor() {
        return sensor;
    }

    public void setSensor(SensorData sensor) {
        this.sensor = sensor;
    }

    public boolean isTrendData() {
        return isTrendData;
    }

    public int getAgeInSensorMinutes() {
        return ageInSensorMinutes;
    }

    public long getDate() {
        return date;
    }


    public int getTimezoneOffsetInMinutes() {
        return timezoneOffsetInMinutes;
    }

    public void setTimezoneOffsetInMinutes(int timezoneOffsetInMinutes) {
        this.timezoneOffsetInMinutes = timezoneOffsetInMinutes;
    }

    int getGlucoseLevelRaw() {
        return glucoseLevelRaw;
    }

    public String getId() {
        return id;
    }
}
