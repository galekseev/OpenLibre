package de.dorianscholz.openlibre.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;
import static de.dorianscholz.openlibre.model.SensorData.maxSensorAgeInMinutes;
import static de.dorianscholz.openlibre.model.SensorData.minSensorAgeInMinutes;
import static java.lang.Math.min;

public class ReadingData extends RealmObject {
    public static final String ID = "id";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_AGE_IN_MINUTES = "sensorAgeInMinutes";
    public static final String DATE = "date";
    public static final String TIMEZONE_OFFSET_IN_MINUTES = "timezoneOffsetInMinutes";
    public static final String TREND = "trend";
    public static final String HISTORY = "history";


    public static final int numHistoryValues = 32;
    public static final int historyIntervalInMinutes = 15;
    public static final int numTrendValues = 16;

    @PrimaryKey
    private String id;
    private SensorData sensor;
    private int sensorAgeInMinutes = -1;
    private long date = -1;
    private int timezoneOffsetInMinutes;
    private RealmList<GlucoseData> trend = new RealmList<>();
    private RealmList<GlucoseData> history = new RealmList<>();

    public ReadingData() {}
    public ReadingData(RawTagData rawTagData) {
        id = rawTagData.getId();
        date = rawTagData.getDate();
        timezoneOffsetInMinutes = rawTagData.getTimezoneOffsetInMinutes();
        sensorAgeInMinutes = rawTagData.getSensorAgeInMinutes();

        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);

        // find or create entry for this sensor
        RealmResults<SensorData> sensors = realmProcessedData.where(SensorData.class).contains(SensorData.ID, rawTagData.getTagId()).findAll();
        if (sensors.size() > 0) {
            sensor = sensors.first();
        } else {
            sensor = new SensorData(rawTagData);
        }

        // check if sensor is of valid age
        if (sensorAgeInMinutes <= minSensorAgeInMinutes || sensorAgeInMinutes > maxSensorAgeInMinutes) {
            realmProcessedData.close();
            return;
        }

        // calculate time drift between sensor readings
        int lastSensorAgeInMinutes = 0;
        long lastReadingDate = sensor.getStartDate();
        RealmResults<ReadingData> readings = realmProcessedData.where(ReadingData.class)
                .contains(ReadingData.ID, rawTagData.getTagId())
                .lessThan(ReadingData.DATE, date)
                .sort(ReadingData.DATE, Sort.DESCENDING)
                .findAll();
        if (readings.size() > 0) {
            lastSensorAgeInMinutes = readings.first().getSensorAgeInMinutes();
            lastReadingDate = readings.first().getDate();
        }
        double timeDriftFactor = 1;
        if (lastSensorAgeInMinutes < sensorAgeInMinutes) {
            timeDriftFactor = (date - lastReadingDate) / (double) TimeUnit.MINUTES.toMillis(sensorAgeInMinutes - lastSensorAgeInMinutes);
        }

        int indexTrend = rawTagData.getIndexTrend();

        int mostRecentHistoryAgeInMinutes = 3 + (sensorAgeInMinutes - 3) % historyIntervalInMinutes;


        // read trend values from ring buffer, starting at indexTrend (bytes 28-123)
        for (int counter = 0; counter < numTrendValues; counter++) {
            int index = (indexTrend + counter) % numTrendValues;

            int glucoseLevelRaw = rawTagData.getTrendValue(index);
            // skip zero values if the sensor has not filled the ring buffer yet completely
            if (glucoseLevelRaw > 0) {
                int dataAgeInMinutes = numTrendValues - counter;
                int ageInSensorMinutes = sensorAgeInMinutes - dataAgeInMinutes;
                long dataDate = lastReadingDate + (long) (TimeUnit.MINUTES.toMillis(ageInSensorMinutes - lastSensorAgeInMinutes) * timeDriftFactor);

                trend.add(new GlucoseData(sensor, ageInSensorMinutes, timezoneOffsetInMinutes, glucoseLevelRaw, true, dataDate));
            }
        }

        int indexHistory = rawTagData.getIndexHistory();

        ArrayList<Integer> glucoseLevels = new ArrayList<>();
        ArrayList<Integer> ageInSensorMinutesList = new ArrayList<>();

        // read history values from ring buffer, starting at indexHistory (bytes 124-315)
        for (int counter = 0; counter < numHistoryValues; counter++) {
            int index = (indexHistory + counter) % numHistoryValues;

            int glucoseLevelRaw = rawTagData.getHistoryValue(index);
            // skip zero values if the sensor has not filled the ring buffer yet completely
            if (glucoseLevelRaw > 0) {
                int dataAgeInMinutes = mostRecentHistoryAgeInMinutes + (numHistoryValues - (counter + 1)) * historyIntervalInMinutes;
                int ageInSensorMinutes = sensorAgeInMinutes - dataAgeInMinutes;

                // skip the first hour of sensor data as it is faulty
                if (ageInSensorMinutes > minSensorAgeInMinutes) {
                    glucoseLevels.add(glucoseLevelRaw);
                    ageInSensorMinutesList.add(ageInSensorMinutes);
                }
            }
        }

        // check if there were actually any valid data points
        if (ageInSensorMinutesList.isEmpty()) {
            realmProcessedData.close();
            return;
        }

        // try to shift age to make this reading fit to older readings
        try {
            shiftAgeToMatchPreviousReadings(realmProcessedData, glucoseLevels, ageInSensorMinutesList);
        } catch (RuntimeException e) {
            Log.e("OpenLibre::ReadingData", e.getMessage() + " For reading with id " + id);
            realmProcessedData.close();
            return;
        }


        // create history data point list
        for (int i = 0; i < glucoseLevels.size(); i++) {
            int glucoseLevelRaw = glucoseLevels.get(i);
            int ageInSensorMinutes = ageInSensorMinutesList.get(i);
            long dataDate = lastReadingDate + (long) (TimeUnit.MINUTES.toMillis(ageInSensorMinutes - lastSensorAgeInMinutes) * timeDriftFactor);

            GlucoseData glucoseData = makeGlucoseData(realmProcessedData, glucoseLevelRaw, ageInSensorMinutes, dataDate);
            if(glucoseData == null) {
                realmProcessedData.close();
                return;
            }
            history.add(glucoseData);
        }

        realmProcessedData.close();
    }

    private GlucoseData makeGlucoseData(Realm realmProcessedData, int glucoseLevelRaw, int ageInSensorMinutes, long dataDate) {
        // if this data point has been read from this sensor before, reuse the object form the database, instead of changing the old data
        RealmResults<GlucoseData> previousGlucoseData = realmProcessedData.where(GlucoseData.class)
                .equalTo(GlucoseData.ID, GlucoseData.generateId(sensor, ageInSensorMinutes, false, glucoseLevelRaw)).findAll();

        // check if a valid previous data point was found
        if (!previousGlucoseData.isEmpty()) {
            if (previousGlucoseData.first().getGlucoseLevelRaw() == glucoseLevelRaw) {
                return previousGlucoseData.first();
            }
            // if the old value does not equal the new one and the sensor has been running for more than three hours, there is an error in the data
            if (ageInSensorMinutes > 3 * minSensorAgeInMinutes) {
                Log.e("OpenLibre::ReadingData", "error in glucose level raw:" + previousGlucoseData.first().getGlucoseLevelRaw() + " != " + glucoseLevelRaw
                        + " for glucose data with id: " + previousGlucoseData.first().getId());
                history.clear();
                trend.clear();
                return null;
            }
        }
        return new GlucoseData(sensor, ageInSensorMinutes, timezoneOffsetInMinutes, glucoseLevelRaw, false, dataDate);
    }

    private void shiftAgeToMatchPreviousReadings(Realm realmProcessedData, ArrayList<Integer> glucoseLevels, ArrayList<Integer> ageInSensorMinutesList) {
        // lookup previous data points from the same sensor and age
        RealmResults<GlucoseData> previousGlucoseDataList = realmProcessedData.where(GlucoseData.class)
                .contains(GlucoseData.ID, sensor.getTagId())
                .equalTo(GlucoseData.IS_TREND_DATA, false)
                .greaterThanOrEqualTo(GlucoseData.AGE_IN_SENSOR_MINUTES, ageInSensorMinutesList.get(0))
                .lessThanOrEqualTo(GlucoseData.AGE_IN_SENSOR_MINUTES, ageInSensorMinutesList.get(ageInSensorMinutesList.size() - 1))
                .sort(GlucoseData.AGE_IN_SENSOR_MINUTES, Sort.ASCENDING)
                .findAll();

        // if there are enough previous and new data points, try to fit them together
        // this is needed as the exact time when a new history value is generated is not known
        // therefore it can happen, that the same data point from two readings would be mapped to different dates
        if (previousGlucoseDataList.size() > 3 && glucoseLevels.size() > 3) {
            int shift = 0;
            boolean equal = listsStartEqual(glucoseLevels, previousGlucoseDataList);
            if (!equal) {
                shift = -1;
                equal = listsStartEqual(glucoseLevels.subList(1, glucoseLevels.size()), previousGlucoseDataList);
                if (!equal) {
                    shift = 1;
                    equal = listsStartEqual(glucoseLevels, previousGlucoseDataList.subList(1, previousGlucoseDataList.size()));
                }
            }
            // if a match between previous and new data points was found, shift the age of the new data points to fit the previous ones
            if (equal) {
                if (shift != 0) {
                    for (int c = 0; c < ageInSensorMinutesList.size(); c++) {
                        ageInSensorMinutesList.set(c, ageInSensorMinutesList.get(c) + shift * historyIntervalInMinutes);
                    }
                }
            } else {
                throw new RuntimeException("No match found between old and new data points.");
            }
        }
    }

    private static boolean listsStartEqual(List<Integer> l1, List<GlucoseData> l2) {
        int size = min(l1.size(), l2.size());
        for (int i = 0; i < size; i++) {
            if (!l1.get(i).equals(l2.get(i).getGlucoseLevelRaw()))
                return false;
        }
        return true;
    }

    public String getId() {
        return id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getTimezoneOffsetInMinutes() {
        return timezoneOffsetInMinutes;
    }

    public void setTimezoneOffsetInMinutes(int timezoneOffsetInMinutes) {
        this.timezoneOffsetInMinutes = timezoneOffsetInMinutes;
    }

    public RealmList<GlucoseData> getTrend() {
        return trend;
    }

    public RealmList<GlucoseData> getHistory() {
        return history;
    }

    public int getSensorAgeInMinutes() {
        return sensorAgeInMinutes;
    }

    public SensorData getSensor() {
        return sensor;
    }

}
