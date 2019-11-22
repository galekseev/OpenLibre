package com.camomile.openlibre.service;

import com.camomile.openlibre.model.AlgorithmUtil;

import java.util.Date;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class PushMessage {

    public enum GlucoseTrend {
        TREND_UP,
        TREND_DOWN,
        TREND_SLIGHTLY_UP,
        TREND_SLIGHTLY_DOWN,
        TREND_STABLE
    }

    private float glucose;
    private float predictedGlucose;
    private double trend;
    private long date;

    public PushMessage(long date, float glucose, float predictedGlucose, double trend){
        this.date = date;
        this.glucose = glucose;
        this.predictedGlucose = predictedGlucose;
        this.trend = trend;
    }

    public float getGlucose() {
        return glucose;
    }

    public float getPredictedGlucose(){
        return predictedGlucose;
    }

    public Date getDate(){
        return new Date(date);
    }

    public GlucoseTrend getTrend() {

        float rotationDegrees = -90f * max(-1f, min(1f, (float) (trend / AlgorithmUtil.TREND_UP_DOWN_LIMIT)));

        if (rotationDegrees < -75)
            return GlucoseTrend.TREND_UP;

        if (rotationDegrees < -15)
            return GlucoseTrend.TREND_SLIGHTLY_UP;

        if (rotationDegrees <= 15)
            return GlucoseTrend.TREND_STABLE;

        if (rotationDegrees <= 75)
            return GlucoseTrend.TREND_SLIGHTLY_DOWN;

        if (rotationDegrees <= 90)
            return GlucoseTrend.TREND_DOWN;

        return GlucoseTrend.TREND_STABLE;
    }
}
