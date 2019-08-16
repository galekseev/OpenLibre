package com.camomile.openlibre.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;

public class LimitAreaLineChart extends LineChart {
    public LimitAreaLineChart(Context context) {
        super(context);
    }

    public LimitAreaLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LimitAreaLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        // replace y-axis renderers
        mAxisRendererLeft = new LimitAreaYAxisRenderer(mViewPortHandler, mAxisLeft, mLeftAxisTransformer);
        mAxisRendererRight = new LimitAreaYAxisRenderer(mViewPortHandler, mAxisRight, mRightAxisTransformer);
    }
}
