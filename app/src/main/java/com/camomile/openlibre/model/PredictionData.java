package com.camomile.openlibre.model;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static java.lang.Math.min;

public class PredictionData {
    private static final int PREDICTION_TIME = 15; // in minutes
    private static final double MAX_CONFIDENCE_INTERVAL = 2;
    public double glucoseSlopeRaw = -1; // mg/dl / 10 minutes
    private double confidenceInterval = -1;
    public GlucoseData glucoseData = new GlucoseData();
    private SimpleRegression regression;

    public PredictionData(List<GlucoseData> trendList) {
        makePrediction(trendList);
    }

    private void makePrediction(List<GlucoseData> trendList) {
        if (trendList.size() == 0) {
            return;
        }
        regression = new SimpleRegression();
        for (int i = 0; i < trendList.size(); i++) {
            regression.addData(i, (trendList.get(i)).getGlucoseLevelRaw());
        }
        int glucoseLevelRaw =
                (int) regression.predict(regression.getN() - 1 + PREDICTION_TIME);
        glucoseSlopeRaw = regression.getSlope();
        confidenceInterval = regression.getSlopeConfidenceInterval();
        int ageInSensorMinutes =
                trendList.get(trendList.size() - 1).getAgeInSensorMinutes() + PREDICTION_TIME;
        glucoseData = new GlucoseData(trendList.get(0).getSensor(), ageInSensorMinutes, trendList.get(0).getTimezoneOffsetInMinutes(), glucoseLevelRaw, true);
    }

    public List<GlucoseData> getPredictedData(int[] ageInSensorMinutesList) {
        int timezoneOffsetInMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000 / 60;
        List<GlucoseData> predictedData = new ArrayList<>();
        for (int ageInSensorMinutes : ageInSensorMinutesList) {
            int glucoseLevelRaw =
                (int) regression.predict(ageInSensorMinutes -
                        (glucoseData.getAgeInSensorMinutes() - (regression.getN() - 1 + PREDICTION_TIME))
                );
            predictedData.add(new GlucoseData(glucoseData.getSensor(), ageInSensorMinutes, timezoneOffsetInMinutes, glucoseLevelRaw, true));
        }
        return predictedData;
    }

    public double confidence() {
        return 1.0 - min(confidenceInterval, MAX_CONFIDENCE_INTERVAL) / MAX_CONFIDENCE_INTERVAL;
    }
}
