package com.camomile.openlibre;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.camomile.openlibre.model.GlucoseData;
import com.camomile.openlibre.model.RawTagData;
import com.camomile.openlibre.model.ReadingData;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.camomile.openlibre.OpenLibre.parseRawData;
import static com.camomile.openlibre.OpenLibre.realmConfigProcessedData;
import static com.camomile.openlibre.OpenLibre.realmConfigRawData;
import static com.camomile.openlibre.OpenLibre.setupRealm;
import static com.camomile.openlibre.model.ReadingData.historyIntervalInMinutes;
import static com.camomile.openlibre.model.ReadingData.numHistoryValues;
import static com.camomile.openlibre.model.ReadingData.numTrendValues;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@RunWith(AndroidJUnit4.class)
public class ReadingDataTest {
    private static final int MAX_READINGS_TO_TEST = 1000;

    private Realm realmRawData;
    private Realm realmProcessedData;
    private ArrayList<ReadingData> readingDataList = new ArrayList<>();
    private ArrayList<RawTagData> rawTagDataList = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Realm.init(context);
        setupRealm(context);

        realmRawData = Realm.getInstance(realmConfigRawData);
    }

    private void parseTestData() {
        // open empty processed data realm for use in parsing data
        Realm.deleteRealm(realmConfigProcessedData);
        realmProcessedData = Realm.getInstance(realmConfigProcessedData);

        // get all raw data
        RealmResults<RawTagData> rawTags = realmRawData.where(RawTagData.class).sort(RawTagData.DATE, Sort.ASCENDING).findAll();

        // reduce data set to just the raw data of the most recent sensor
        String tagId = rawTags.last().getTagId();
        rawTags = rawTags.where().equalTo(RawTagData.TAG_ID, tagId).sort(RawTagData.DATE, Sort.ASCENDING).findAll();

        // reduce data set further to only MAX_READINGS_TO_TEST sensor readings
        for (int i = 0; i < min(MAX_READINGS_TO_TEST, rawTags.size()); i++) {
            addDataIfValid(rawTags.get(i));
        }

        /*
        // add oldest readings of sensor
        for (int i = 0; i < min(MAX_READINGS_TO_TEST / 2, rawTags.size() / 2); i++) {
            addDataIfValid(rawTags.get(i));
        }
        // add newest readings of sensor
        for (int i = max(rawTags.size() / 2, rawTags.size() - 1 - MAX_READINGS_TO_TEST / 2); i < rawTags.size() - 1; i++) {
            addDataIfValid(rawTags.get(i));
        }
        */

        assertThat(realmRawData.isEmpty(), is(false));
        assertThat(readingDataList.size(), greaterThan(0));
    }

    private void addDataIfValid(RawTagData rawTagData) {
        // check if sensor has been initialized and is not yet over due date
        if (rawTagData.getSensorAgeInMinutes() < numHistoryValues * historyIntervalInMinutes ||
                rawTagData.getSensorAgeInMinutes() > TimeUnit.DAYS.toMinutes(14))
            return;

        // check if data contains enough history and trend data for the tests to work
        ReadingData readingData = new ReadingData(rawTagData);
        if (readingData.getHistory().size() < numHistoryValues ||
                readingData.getTrend().size() != numTrendValues) {
            //Log.d("OpenLibre::Test", "history size: " + readingData.getHistory().size() + "  trend size: " + readingData.getTrend().size());
            return;
        }

        // add reading to realm, so when parsing the next reading, it can access this data
        realmProcessedData.beginTransaction();
        realmProcessedData.copyToRealmOrUpdate(readingData);
        realmProcessedData.commitTransaction();

        rawTagDataList.add(rawTagData);
        readingDataList.add(readingData);
    }

    @After
    public void tearDown() {
        realmRawData.close();
        // if any test opened the processed data realm, close it and delete the data
        if (realmProcessedData != null) {
            realmProcessedData.close();
            Realm.deleteRealm(realmConfigProcessedData);
        }
    }

    //@Ignore
    @Test
    public void testReparseAllData() {
        // delete all parsed readings before the tests
        Realm.deleteRealm(realmConfigProcessedData);

        // reparse all readings from raw data
        parseRawData();

        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        assertThat(realmProcessedData.isEmpty(), is(false));
        realmProcessedData.close();
    }

    @Test
    public void testTrendIndexVsSensorAge() {
        parseTestData();
        ArrayList<Integer> results = new ArrayList<>();

        for (RawTagData rawTagData : rawTagDataList) {
            int diff = ((rawTagData.getSensorAgeInMinutes() % numTrendValues) - rawTagData.getIndexTrend() + numTrendValues) % numTrendValues;
            results.add(diff);
            if (diff > 1) Log.w("OpenLibre::TEST", "failed for: sensorAge: " + rawTagData.getSensorAgeInMinutes() + "  trendIndex: " + rawTagData.getIndexTrend());
        }
        Log.d("OpenLibre::TEST", "sensorAge % numTrendValues - trendIndex:  " + results);

        assertThat("trend index drifted away from sensor age more than one minute", results, everyItem(lessThanOrEqualTo(1)));
    }

    @Test
    public void testHistoryDatesMatchOnOverlappingReadings() {
        parseTestData();
        ArrayList<Integer> results = new ArrayList<>();

        ReadingData oldReadingData = readingDataList.get(0);
        results.add(0);
        for (ReadingData readingData : readingDataList.subList(1, readingDataList.size())) {
            GlucoseData oldGlucoseData = oldReadingData.getHistory().last();
            boolean found = false;
            for (GlucoseData glucoseData : readingData.getHistory()) {
                if (oldGlucoseData.glucose() == glucoseData.glucose()) {
                    if (oldGlucoseData.getAgeInSensorMinutes() - glucoseData.getAgeInSensorMinutes() == 0) {
                        // well matched
                        results.add(0);
                        found = true;
                        break;
                    } else if (abs(oldGlucoseData.getAgeInSensorMinutes() - glucoseData.getAgeInSensorMinutes()) < historyIntervalInMinutes) {
                        results.add(oldGlucoseData.getAgeInSensorMinutes() - glucoseData.getAgeInSensorMinutes());
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
                results.add(0);
            oldReadingData = readingData;
        }
        Log.d("OpenLibre::TEST", "age diff:  " + results);

        assertThat("history dates on overlapping readings don't match", results, everyItem(equalTo(0)));
        assertThat("no overlapping readings found", results.size(), greaterThan(0));
    }

    @Test
    public void testHistoryDatesFitTrendData() {
        parseTestData();
        ArrayList<Integer> firstTrendResults = new ArrayList<>();
        ArrayList<Integer> lastTrendResults = new ArrayList<>();

        for (ReadingData readingData : readingDataList) {
            GlucoseData lastHistory = readingData.getHistory().get(readingData.getHistory().size() - 1);
            GlucoseData firstTrend = readingData.getTrend().get(0);
            GlucoseData lastTrend = readingData.getTrend().get(readingData.getTrend().size() - 1);

            long lastHistoryMinutes = TimeUnit.MILLISECONDS.toMinutes(lastHistory.getDate());
            long lastTrendMinutes = TimeUnit.MILLISECONDS.toMinutes(lastTrend.getDate());
            long firstTrendMinutes = TimeUnit.MILLISECONDS.toMinutes(firstTrend.getDate());

            firstTrendResults.add((int)(lastHistoryMinutes - firstTrendMinutes));
            lastTrendResults.add((int)(lastTrendMinutes - lastHistoryMinutes));
        }
        Log.d("OpenLibre::TEST", "last history - first trend date: " + firstTrendResults);
        Log.d("OpenLibre::TEST", "last trend - last history date:  " + lastTrendResults);

        assertThat("history ends more than 3 minutes before first trend", firstTrendResults, everyItem(greaterThanOrEqualTo(-3)));
        assertThat("history ends after last trend", lastTrendResults, everyItem(greaterThanOrEqualTo(0)));
    }

    @Test
    public void testHistoryValuesFitTrendData() {
        parseTestData();
        ArrayList<Float> minTrendResults = new ArrayList<>();
        ArrayList<Float> maxTrendResults = new ArrayList<>();

        for (ReadingData readingData : readingDataList) {
            GlucoseData lastHistory = readingData.getHistory().get(readingData.getHistory().size() - 1);
            ArrayList<Float> trendValues = new ArrayList<>();
            for (GlucoseData glucoseData : readingData.getTrend()) {
                trendValues.add(glucoseData.glucose(false));
            }

            minTrendResults.add(lastHistory.glucose(false) - Collections.min(trendValues));
            maxTrendResults.add(Collections.max(trendValues) - lastHistory.glucose(false));
        }
        Log.d("OpenLibre::TEST", "last history - min trend value: " + minTrendResults);
        Log.d("OpenLibre::TEST", "max trend value - last history: " + maxTrendResults);

        assertThat("last history value far smaller than trend values", minTrendResults, everyItem(greaterThanOrEqualTo(-10f)));
        assertThat("last history value far greater than trend values", maxTrendResults, everyItem(greaterThanOrEqualTo(-10f)));
    }

}
