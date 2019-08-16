package com.camomile.openlibre.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.camomile.openlibre.model.AlgorithmUtil;
import com.camomile.openlibre.model.GlucoseData;
import com.camomile.openlibre.model.PredictionData;
import com.camomile.openlibre.model.ReadingData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.camomile.openlibre.R;

import static com.camomile.openlibre.OpenLibre.GLUCOSE_TARGET_MAX;
import static com.camomile.openlibre.OpenLibre.GLUCOSE_TARGET_MIN;
import static com.camomile.openlibre.OpenLibre.GLUCOSE_UNIT_IS_MMOL;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class DataPlotFragment extends Fragment
        implements OnChartValueSelectedListener, OnChartGestureListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_ID = "OpenLibre::" + DataPlotFragment.class.getSimpleName();

    private final static int NUM_PLOT_COLORS = 3;
    private final static int[][] PLOT_COLORS = new int[][] {
            {Color.BLUE, Color.BLUE},
            {Color.MAGENTA, Color.RED},
            {Color.CYAN, Color.GREEN},
    };
    private static int mPlotColorIndex = 0;
    private final static int maxZoomFactor = 12;

    private View mDataPlotView;
    LineChart mPlot;
    private long mFirstDate = -1;
    private Timer mUpdatePlotTitleTimer;
    private TimerTask mUpdatePlotTitleTask = null;
    private DateTimeMarkerView mDateTimeMarkerView;
    boolean isZoomedToTrend = false;

    @SuppressWarnings("unused")
    public static DataPlotFragment newInstance() {
        return new DataPlotFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDataPlotView = inflater.inflate(R.layout.fragment_scan, container, false);
        resetView();

        mUpdatePlotTitleTimer = new Timer();

        setupPlot();

        clearScanData();

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);

        mDataPlotView.findViewById(R.id.iv_glucose_prediction).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (isZoomedToTrend)
                    zoomOutMax();
                else
                    zoomToTrend();
            }
        });


        return mDataPlotView;
    }

    private void resetView() {
        ((ProgressBar) mDataPlotView.findViewById(R.id.pb_scan_circle)).setProgress(0);
        mDataPlotView.findViewById(R.id.scan_progress).setVisibility(View.VISIBLE);
        mDataPlotView.findViewById(R.id.scan_view).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mUpdatePlotTitleTask != null) {
            mUpdatePlotTitleTask.cancel();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_glucose_unit_is_mmol") || key.equals("pref_glucose_target_min") || key.equals("pref_glucose_target_max")) {
            resetView();
        }
    }

    private void setupPlot() {
        mPlot = (LineChart) mDataPlotView.findViewById(R.id.cv_glucose_plot);
        mPlot.setNoDataText("");
        mPlot.setOnChartGestureListener(this);
        mDateTimeMarkerView = new DateTimeMarkerView(getContext(), R.layout.date_time_marker);
        mPlot.setMarker(mDateTimeMarkerView);
        mDateTimeMarkerView.setChartView(mPlot);

        // prevent scrolling in the plot to trigger switching of the view pager tabs
        mPlot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.view_pager);
                        viewPager.requestDisallowInterceptTouchEvent(true);
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.view_pager);
                        viewPager.requestDisallowInterceptTouchEvent(false);
                        break;
                    }
                }
                return false;
            }
        });

        // no description text
        mPlot.getDescription().setEnabled(false);

        // enable touch gestures
        mPlot.setTouchEnabled(true);
        mPlot.setDragDecelerationFrictionCoef(0.9f);

        // if disabled, scaling can be done on x- and y-axis separately
        mPlot.setPinchZoom(true);

        // enable scaling and dragging
        mPlot.setDragEnabled(true);
        mPlot.setScaleEnabled(true);
        mPlot.setDrawGridBackground(false);

        // set an alternative background color
        mPlot.setBackgroundColor(Color.argb(0, 255, 255, 255));

        mPlot.setOnChartValueSelectedListener(this);

        Legend legend = mPlot.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = mPlot.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(false);
        xAxis.setGranularity(convertDateToXAxisValue(TimeUnit.MINUTES.toMillis(5L))); // same unit as x axis values
        xAxis.setDrawLimitLinesBehindData(true);
        xAxis.setLabelCount(4);

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                long date = convertXAxisValueToDate(value);
                return AlgorithmUtil.mFormatTimeShort.format(new Date(date));
            }
        });

        YAxis leftAxis = mPlot.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextSize(12f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(5f);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(500f);

        YAxis rightAxis = mPlot.getAxisRight();
        rightAxis.setEnabled(false);

        updateTargetArea();

        try {
            mPlot.setHardwareAccelerationEnabled(true);
        } catch (Exception e) {
            Log.d(LOG_ID, "Hardware acceleration for data plot failed: " + e.toString());
        }
    }

    private void updateTargetArea() {
        YAxis leftAxis = mPlot.getAxisLeft();
        leftAxis.removeAllLimitLines();
        LimitLine limitLineMax = new LimitLine(
                GLUCOSE_TARGET_MAX
        );
        limitLineMax.setLineColor(Color.TRANSPARENT);
        leftAxis.addLimitLine(limitLineMax);

        LimitLine limitLineMin = new LimitLine(
                GLUCOSE_TARGET_MIN,
                getResources().getString(R.string.pref_glucose_target_area)
        );
        limitLineMin.setTextSize(10f);
        limitLineMin.setLineColor(Color.argb(60, 100, 100, 120));
        limitLineMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        leftAxis.addLimitLine(limitLineMin);
    }

    public void clearScanData() {
        mDataPlotView.findViewById(R.id.scan_data).setVisibility(View.GONE);
    }

    public void showMultipleScans(List<ReadingData> readingDataList) {
        updateTargetArea();
        mPlot.clear();
        mDataPlotView.findViewById(R.id.scan_progress).setVisibility(View.INVISIBLE);
        mDataPlotView.findViewById(R.id.scan_view).setVisibility(View.VISIBLE);

        for (ReadingData readingData : readingDataList) {
            addLineData(readingData.getHistory(), readingData.getTrend());
        }

        updatePlotTitle(false);
        updateChartViewConstrains();
        ((TextView) mDataPlotView.findViewById(R.id.tv_plot_date)).setText("");
    }

    void showHistory(List<GlucoseData> history) {
        updateTargetArea();
        mPlot.clear();
        mDataPlotView.findViewById(R.id.scan_progress).setVisibility(View.INVISIBLE);
        mDataPlotView.findViewById(R.id.scan_view).setVisibility(View.VISIBLE);

        updatePlot(history, null);
    }

    void showScan(ReadingData readData) {
        updateTargetArea();
        mPlot.clear();
        mDataPlotView.findViewById(R.id.scan_progress).setVisibility(View.INVISIBLE);
        mDataPlotView.findViewById(R.id.scan_view).setVisibility(View.VISIBLE);

        updateScanData(readData.getTrend());
        updatePlot(readData.getHistory(), readData.getTrend());
    }

    private void updateScanData(List<GlucoseData> trend) {
        if (trend.size() == 0) {
            Toast.makeText(this.getContext(), "No current data available!", Toast.LENGTH_LONG).show();
            return;
        }

        mDataPlotView.findViewById(R.id.scan_data).setVisibility(View.VISIBLE);

        GlucoseData currentGlucose = trend.get(trend.size() - 1);
        TextView tv_currentGlucose = (TextView) mDataPlotView.findViewById(R.id.tv_glucose_current_value);
        tv_currentGlucose.setText(
                String.format(getResources().getString(R.string.glucose_current_value),
                currentGlucose.glucoseString(),
                GlucoseData.getDisplayUnit())
        );

        PredictionData predictedGlucose = new PredictionData(trend);

        TextView tv_predictedGlucose = (TextView) mDataPlotView.findViewById(R.id.tv_glucose_prediction);
        tv_predictedGlucose.setText(String.valueOf(predictedGlucose.glucoseData.glucoseString()));
        tv_predictedGlucose.setAlpha((float) min(1, 0.1 + predictedGlucose.confidence()));

        ImageView iv_unit = (ImageView) mDataPlotView.findViewById(R.id.iv_unit);
        if (GLUCOSE_UNIT_IS_MMOL) {
            iv_unit.setImageResource(R.drawable.ic_unit_mmoll);
        } else {
            iv_unit.setImageResource(R.drawable.ic_unit_mgdl);
        }
        iv_unit.setAlpha((float) min(1, 0.1 + predictedGlucose.confidence()));

        ImageView iv_predictionArrow = (ImageView) mDataPlotView.findViewById(R.id.iv_glucose_prediction);

        // rotate trend arrow according to glucose prediction slope
        float rotationDegrees = -90f * max(-1f, min(1f, (float) (predictedGlucose.glucoseSlopeRaw / AlgorithmUtil.TREND_UP_DOWN_LIMIT)));
        iv_predictionArrow.setRotation(rotationDegrees);

        // reduce trend arrow visibility according to prediction confidence
        iv_predictionArrow.setAlpha((float) min(1, 0.1 + predictedGlucose.confidence()));
    }

    private void updatePlot(List<GlucoseData> history, List<GlucoseData> trend) {
        Log.d(LOG_ID, String.format("#history: %d, #trend: %d", history.size(), trend == null ? 0 : trend.size()));

        if (history.size() == 0) {
            Toast.makeText(this.getContext(), "No historical data available!", Toast.LENGTH_LONG).show();
            return;
        }

        mPlotColorIndex = 0;
        addLineData(history, trend);

        updatePlotTitle(trend != null);
        updateChartViewConstrains();
        ((TextView) mDataPlotView.findViewById(R.id.tv_plot_date)).setText("");
    }

    private void addLineData(List<GlucoseData> history, List<GlucoseData> trend) {
        if (mFirstDate < 0) {
            mFirstDate = history.get(0).getDate();
            mDateTimeMarkerView.setFirstDate(mFirstDate);
        }

        LineData lineData = mPlot.getData();
        if (lineData == null) {
            lineData = new LineData();
        }
        lineData.addDataSet(makeLineData(history));
        if (trend != null) {
            /*
            // connect history and trend data
            List<GlucoseData> connection = new ArrayList<>();
            connection.add(history.get(history.size() - 1));
            connection.add(trend.get(0));
            lineData.addDataSet(makeLineData(connection));
            */

            // show trend data
            lineData.addDataSet(makeLineData(trend));

            /*
            // also show regression data
            PredictionData predictionData = new PredictionData(trend);
            List<GlucoseData> prediction = predictionData.getPredictedData(
                    new int[]{trend.get(0).ageInSensorMinutes, trend.get(trend.size() - 1).ageInSensorMinutes});
            lineData.addDataSet(makeLineData(prediction));
            */
        }
        mPlotColorIndex++;
        mPlot.setData(lineData);
    }

    private void setPlotTitleUpdateTimer() {
        // update 3 minutes after most recent data timestamp
        Date updateTime = new Date(3 * 60 * 1000 + convertXAxisValueToDate(mPlot.getData().getXMax()));
        if (mUpdatePlotTitleTask != null) {
            mUpdatePlotTitleTask.cancel();
        }
        mUpdatePlotTitleTask = new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            TextView tv_plotTitle = (TextView) mDataPlotView.findViewById(R.id.tv_plot_title);
                            tv_plotTitle.setTextColor(Color.RED);
                        }
                    });
                }
            }
        };
        mUpdatePlotTitleTimer.schedule(mUpdatePlotTitleTask, updateTime);
    }

    private void updatePlotTitle(boolean isScanData) {
        TextView tv_plotTitle = (TextView) mDataPlotView.findViewById(R.id.tv_plot_title);
        String plotTitle;
        if (isScanData) {
            plotTitle = AlgorithmUtil.mFormatDateTime.format(new Date(convertXAxisValueToDate(mPlot.getData().getXMax())));
        } else {
            plotTitle = String.format("Data from %s to %s",
                    AlgorithmUtil.mFormatDateTime.format(new Date(convertXAxisValueToDate(mPlot.getData().getXMin()))),
                    AlgorithmUtil.mFormatDateTime.format(new Date(convertXAxisValueToDate(mPlot.getData().getXMax()))));
        }
        tv_plotTitle.setTextColor(Color.BLACK);
        setPlotTitleUpdateTimer();
        tv_plotTitle.setText(plotTitle);
    }

    private void updateChartViewConstrains() {
        mPlot.fitScreen();

        final float minGlucoseShown = GlucoseData.convertGlucoseMGDLToDisplayUnit(20);
        final float maxGlucoseShown = minGlucoseShown * maxZoomFactor;

        mPlot.setVisibleYRangeMinimum(minGlucoseShown, mPlot.getAxisLeft().getAxisDependency());
        mPlot.setVisibleYRangeMaximum(maxGlucoseShown, mPlot.getAxisLeft().getAxisDependency());

        final float maxMinutesShown = ReadingData.historyIntervalInMinutes * ReadingData.numHistoryValues + 2 * ReadingData.numTrendValues;
        final float minMinutesShown = maxMinutesShown / maxZoomFactor;

        mPlot.setVisibleXRangeMinimum(minMinutesShown);
        mPlot.setVisibleXRangeMaximum(maxMinutesShown);

        zoomOutMax();

        mPlot.invalidate();
    }

    private void zoomOutMax() {
        mPlot.zoom(1 / maxZoomFactor, 1 / maxZoomFactor, mPlot.getData().getXMax(), (mPlot.getData().getYMax() + mPlot.getData().getYMin()) / 2, mPlot.getAxisLeft().getAxisDependency());
        isZoomedToTrend = false;
    }

    private void zoomToTrend() {
        ILineDataSet lineDataSet = mPlot.getData().getDataSetByIndex(mPlot.getData().getDataSetCount() - 1);
        Entry lastEntry = lineDataSet.getEntryForIndex(lineDataSet.getEntryCount() - 1);
        float yCenter = (lineDataSet.getYMin() + lineDataSet.getYMax()) / 2;

        // zoom in max to the last data points
        mPlot.zoom(maxZoomFactor, maxZoomFactor, lastEntry.getX(), yCenter, mPlot.getAxisLeft().getAxisDependency());
        isZoomedToTrend = true;
    }

    private LineDataSet makeLineData(List<GlucoseData> glucoseDataList) {
        String title = "History";
        if (glucoseDataList.get(0).isTrendData()) title = "Trend";

        LineDataSet lineDataSet = new LineDataSet(new ArrayList<Entry>(), title);
        for (GlucoseData gd : glucoseDataList) {
            float x = convertDateToXAxisValue(gd.getDate());
            float y = gd.glucose();
            lineDataSet.addEntryOrdered(new Entry(x, y));
            /*
            Log.d(LOG_ID, String.format("%s: %s -> %s: %f -> %f",
                    title,
                    mFormatDateTime.format(new Date(gd.date)),
                    mFormatDateTime.format(new Date(convertXAxisValueToDate(x))),
                    x,
                    y)
            );
            */
        }

        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleRadius(2f);

        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawValues(false);

        lineDataSet.setDrawHighlightIndicators(true);

        int baseColor = PLOT_COLORS[mPlotColorIndex % NUM_PLOT_COLORS][0];
        int softColor = Color.argb(150, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        int hardColor = PLOT_COLORS[mPlotColorIndex % NUM_PLOT_COLORS][1];
        if (glucoseDataList.get(0).isTrendData()) {
            lineDataSet.setColor(hardColor);
            lineDataSet.setLineWidth(2f);

            lineDataSet.setCircleColor(softColor);

            lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        } else {
            lineDataSet.setColor(softColor);
            lineDataSet.setLineWidth(4f);

            lineDataSet.setCircleColor(hardColor);

            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet.setCubicIntensity(0.1f);
        }

        return lineDataSet;
    }

    private float convertDateToXAxisValue(long date) {
        return (date - mFirstDate) / TimeUnit.MINUTES.toMillis(1L);
    }

    private long convertXAxisValueToDate(float value) {
        return mFirstDate + (long) (value * TimeUnit.MINUTES.toMillis(1L));
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.d(LOG_ID, "Selected: " + e.toString() + " : " +
                AlgorithmUtil.mFormatDateTime.format(new Date(convertXAxisValueToDate(e.getX()))));
    }

    @Override
    public void onNothingSelected() {

    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        updatePlotDate();
    }

    public void updatePlotDate() {
        TextView plotTitle = (TextView) mDataPlotView.findViewById(R.id.tv_plot_date);
        if (convertXAxisValueToDate(mPlot.getData().getXMax()) -
                convertXAxisValueToDate(mPlot.getData().getXMin())
                > TimeUnit.HOURS.toMillis(12L)) {
            String minDate = AlgorithmUtil.mFormatDate.format(new Date(convertXAxisValueToDate(mPlot.getLowestVisibleX())));
            String maxDate = AlgorithmUtil.mFormatDate.format(new Date(convertXAxisValueToDate(mPlot.getHighestVisibleX())));
            if (minDate.compareTo(maxDate) == 0 || mPlot.getLowestVisibleX() > mPlot.getHighestVisibleX()) {
                plotTitle.setText(maxDate);
            } else {
                plotTitle.setText(minDate + " - " + maxDate);
            }
        } else {
            // no need to show the date, if showing less then 12 hours of data (e.g. a single scan)
            plotTitle.setText("");
        }
    }

}
