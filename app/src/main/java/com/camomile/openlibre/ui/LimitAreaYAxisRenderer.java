package com.camomile.openlibre.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

public class LimitAreaYAxisRenderer extends YAxisRenderer {

    public LimitAreaYAxisRenderer(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
        super(viewPortHandler, yAxis, trans);
    }

    @Override
    public void renderLimitLines(Canvas c) {

        List<LimitLine> limitLines = mYAxis.getLimitLines();

        if (limitLines == null || limitLines.size() <= 0)
            return;

        float[] pts = mRenderLimitLinesBuffer;
        pts[0] = 0;
        pts[1] = 0;
        float upperLimit = Float.NaN;

        for (int i = 0; i < limitLines.size(); i++) {

            LimitLine l = limitLines.get(i);

            if (!l.isEnabled())
                continue;

            int clipRestoreCount = c.save();
            mLimitLineClippingRect.set(mViewPortHandler.getContentRect());
            c.clipRect(mLimitLineClippingRect);

            mLimitLinePaint.setStyle(Paint.Style.FILL);
            mLimitLinePaint.setColor(l.getLineColor());

            pts[1] = l.getLimit();

            mTrans.pointValuesToPixel(pts);

            if (Float.isNaN(upperLimit)) {
                upperLimit = pts[1];
            } else {
                float lowerLimit = pts[1];
                if (upperLimit < lowerLimit) {
                    lowerLimit = upperLimit;
                    upperLimit = pts[1];
                }
                c.drawRect(mViewPortHandler.contentLeft(), lowerLimit, mViewPortHandler.contentRight(), upperLimit, mLimitLinePaint);
                upperLimit = Float.NaN;
            }

            c.restoreToCount(clipRestoreCount);
        }

        super.renderLimitLines(c);
    }
}
