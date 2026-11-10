package org.gitzian.neurogranny.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.model.ReviewEvent;

/**
 * Draws recall-history charts. Data aggregation and chart rendering are
 * separate so calendar counting can be tested without Android graphics APIs.
 */
public final class ReviewChartRenderer {
    private static final int TEXT_COLOR = 0xffa4a0a9;
    private static final int OUTLINE_COLOR = 0xff38303d;
    private static final int GRID_COLOR = 0xff302a35;
    private static final int CORRECT_COLOR = 0xffc8c4ce;
    private static final int INCORRECT_COLOR = 0xff9675b0;

    private static final float LEFT_PADDING_DP = 30.0f;
    private static final float TOP_PADDING_DP = 38.0f;
    private static final float BOTTOM_PADDING_DP = 25.0f;
    private static final float RIGHT_PADDING_DP = 9.0f;
    private static final float LEGEND_SWATCH_DP = 8.0f;
    private static final float AXIS_LABEL_SIZE_DP = 10.0f;
    private static final float DATE_LABEL_SIZE_DP = 9.0f;
    private static final float EMPTY_LABEL_SIZE_DP = 12.0f;
    private static final float POINT_RADIUS_DP = 3.0f;
    private static final float CURVE_STROKE_WIDTH_DP = 2.0f;
    private static final float MAX_BAR_WIDTH_DP = 9.0f;
    private static final float MIN_BAR_HEIGHT_DP = 2.0f;

    private final Context context;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final UiTypography typography;

    private Canvas canvas;
    private Locale locale;

    public ReviewChartRenderer(Context context, UiTypography typography) {
        this.context = context;
        this.typography = typography;
    }

    /**
     * Draws correct recalls and incorrect password submissions in adjacent bars.
     * Calendar-aligned aggregation is shared by the detail and history screens.
     */
    public void draw(Canvas target, List<ReviewEvent> events, float x, float y,
            float width, float height, boolean curve, long start, long end,
            Locale locale) {
        this.canvas = target;
        this.locale = locale;
        ReviewChartData chart = ReviewChartData.aggregate(events, start, end,
                TimeZone.getDefault(), locale);
        drawBorder(x, y, width, height);
        drawChartLegend(x, y, width);

        float plotLeft = x + LEFT_PADDING_DP;
        float plotRight = x + width - RIGHT_PADDING_DP;
        float plotTop = y + TOP_PADDING_DP;
        float plotBottom = y + height - BOTTOM_PADDING_DP;
        float plotWidth = Math.max(1.0f, plotRight - plotLeft);
        float plotHeight = Math.max(1.0f, plotBottom - plotTop);
        int maximum = Math.max(1, chart.maximumCount());

        drawLine(plotLeft, plotBottom, plotRight, plotBottom, GRID_COLOR);
        drawChartLabel("0", x + 6.0f, plotBottom + 3.0f, AXIS_LABEL_SIZE_DP);

        if (chart.isEmpty()) {
            drawCenteredChartLabel(context.getString(R.string.no_reviews_yet),
                    x + width / 2.0f, plotTop + plotHeight / 2.0f, EMPTY_LABEL_SIZE_DP);
            drawChartDateLabels(chart, plotLeft, plotRight, plotBottom + 17.0f);
            return;
        }

        drawLine(plotLeft, plotTop, plotRight, plotTop, GRID_COLOR);
        drawChartLabel(String.valueOf(maximum), x + 6.0f,
                plotTop + 4.0f, AXIS_LABEL_SIZE_DP);
        if (maximum >= 2) {
            int middle = maximum / 2;
            float middleY = plotBottom - plotHeight * middle / maximum;
            drawLine(plotLeft, middleY, plotRight, middleY, GRID_COLOR);
            drawChartLabel(String.valueOf(middle), x + 6.0f,
                    middleY + 3.0f, AXIS_LABEL_SIZE_DP);
        }

        if (curve) {
            drawChartCurves(chart, plotLeft, plotBottom, plotWidth, plotHeight, maximum);
        } else {
            drawChartBars(chart, plotLeft, plotBottom, plotWidth, plotHeight, maximum);
        }
        drawChartDateLabels(chart, plotLeft, plotRight, plotBottom + 17.0f);
    }

    private void drawChartLegend(float x, float y, float width) {
        float firstX = x + 11.0f;
        float secondX = x + width / 2.0f;
        drawChartKey(firstX, y + 11.0f, CORRECT_COLOR,
                context.getString(R.string.chart_correct), secondX - firstX - 7.0f);
        drawChartKey(secondX, y + 11.0f, INCORRECT_COLOR,
                context.getString(R.string.chart_incorrect), x + width - secondX - 8.0f);
    }

    private void drawChartKey(float x, float y, int color, String label, float availableWidth) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(x, y, x + LEGEND_SWATCH_DP, y + LEGEND_SWATCH_DP, paint);

        canvas.save();
        canvas.clipRect(x + 12.0f, y - 3.0f,
                x + Math.max(12.0f, availableWidth), y + 13.0f);
        drawChartLabel(label, x + 12.0f, y + 8.0f, AXIS_LABEL_SIZE_DP);
        canvas.restore();
    }

    private void drawChartBars(ReviewChartData chart, float plotLeft,
            float plotBottom, float plotWidth, float plotHeight, int maximum) {
        List<ReviewChartData.Bucket> buckets = chart.buckets();
        float groupWidth = plotWidth / buckets.size();
        float groupGap = Math.min(3.0f, groupWidth * 0.12f);
        float barWidth = Math.max(1.0f, Math.min(MAX_BAR_WIDTH_DP,
                (groupWidth - groupGap) / 2.0f - 0.5f));

        paint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < buckets.size(); index++) {
            ReviewChartData.Bucket bucket = buckets.get(index);
            float centerX = plotLeft + (index + 0.5f) * groupWidth;
            drawChartBar(centerX - groupGap / 2.0f - barWidth,
                    plotBottom, barWidth, plotHeight, bucket.correct, maximum, CORRECT_COLOR);
            drawChartBar(centerX + groupGap / 2.0f,
                    plotBottom, barWidth, plotHeight, bucket.incorrect, maximum, INCORRECT_COLOR);
        }
    }

    private void drawChartBar(float x, float bottom, float width, float plotHeight,
            int count, int maximum, int color) {
        if (count == 0) {
            return;
        }
        float height = Math.max(MIN_BAR_HEIGHT_DP, plotHeight * count / maximum);
        paint.setColor(color);
        canvas.drawRect(x, bottom - height, x + width, bottom, paint);
    }

    private void drawChartCurves(ReviewChartData chart, float plotLeft,
            float plotBottom, float plotWidth, float plotHeight, int maximum) {
        List<ReviewChartData.Bucket> buckets = chart.buckets();
        float groupWidth = plotWidth / buckets.size();

        // A single populated bucket has no meaningful connecting curve.
        boolean drawConnectingLines = chart.populatedBuckets() > 1;
        if (drawConnectingLines) {
            drawChartCurvePath(buckets, plotLeft, plotBottom, plotHeight,
                    groupWidth, maximum, true, CORRECT_COLOR);
            drawChartCurvePath(buckets, plotLeft, plotBottom, plotHeight,
                    groupWidth, maximum, false, INCORRECT_COLOR);
        }

        for (int index = 0; index < buckets.size(); index++) {
            ReviewChartData.Bucket bucket = buckets.get(index);
            float centerX = plotLeft + (index + 0.5f) * groupWidth;
            drawChartPoint(centerX, plotBottom, plotHeight,
                    bucket.correct, maximum, CORRECT_COLOR);
            drawChartPoint(centerX, plotBottom, plotHeight,
                    bucket.incorrect, maximum, INCORRECT_COLOR);
        }
    }

    private void drawChartCurvePath(List<ReviewChartData.Bucket> buckets,
            float plotLeft, float plotBottom, float plotHeight, float groupWidth,
            int maximum, boolean correct, int color) {
        Path path = new Path();
        for (int index = 0; index < buckets.size(); index++) {
            ReviewChartData.Bucket bucket = buckets.get(index);
            int count = correct ? bucket.correct : bucket.incorrect;
            float x = plotLeft + (index + 0.5f) * groupWidth;
            float y = plotBottom - plotHeight * count / maximum;
            if (index == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setColor(color);
        paint.setStrokeWidth(CURVE_STROKE_WIDTH_DP);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawChartPoint(float centerX, float plotBottom, float plotHeight,
            int count, int maximum, int color) {
        if (count <= 0) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float centerY = plotBottom - plotHeight * count / maximum;
        canvas.drawCircle(centerX, centerY, POINT_RADIUS_DP, paint);
    }

    private void drawChartDateLabels(ReviewChartData chart, float plotLeft,
            float plotRight, float baseline) {
        List<ReviewChartData.Bucket> buckets = chart.buckets();
        if (buckets.isEmpty()) {
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat(
                chart.resolution() == ReviewChartData.Resolution.HOUR ? "HH:mm"
                        : chart.resolution() == ReviewChartData.Resolution.MONTH ? "MMM yy" : "d MMM",
                locale);
        float groupWidth = (plotRight - plotLeft) / buckets.size();
        String first = format.format(new Date(buckets.get(0).startMillis));
        String last = format.format(new Date(buckets.get(buckets.size() - 1).startMillis));
        drawChartLabel(first, plotLeft, baseline, DATE_LABEL_SIZE_DP);
        if (buckets.size() > 1) {
            drawRightAlignedChartLabel(last, plotRight, baseline, DATE_LABEL_SIZE_DP);
        }
        if (buckets.size() >= 8) {
            int midpoint = buckets.size() / 2;
            String middle = format.format(new Date(buckets.get(midpoint).startMillis));
            float middleX = plotLeft + (midpoint + 0.5f) * groupWidth;
            drawCenteredChartLabel(middle, middleX, baseline, DATE_LABEL_SIZE_DP);
        }
    }

    private void drawChartLabel(String value, float x, float baseline, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TEXT_COLOR);
        paint.setTypeface(typography.normal());
        paint.setTextSize(typography.spToDp(size));
        canvas.drawText(value, x, baseline, paint);
    }

    private void drawCenteredChartLabel(String value, float centerX, float baseline,
            float size) {
        paint.setTypeface(typography.normal());
        paint.setTextSize(typography.spToDp(size));
        drawChartLabel(value, centerX - paint.measureText(value) / 2.0f, baseline, size);
    }

    private void drawRightAlignedChartLabel(String value, float rightX, float baseline,
            float size) {
        paint.setTypeface(typography.normal());
        paint.setTextSize(typography.spToDp(size));
        drawChartLabel(value, rightX - paint.measureText(value), baseline, size);
    }

    private void drawBorder(float x, float y, float width, float height) {
        paint.setColor(OUTLINE_COLOR);
        paint.setStrokeWidth(1.0f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height),
                5.0f, 5.0f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLine(float x1, float y1, float x2, float y2, int color) {
        paint.setColor(color);
        paint.setStrokeWidth(1.0f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
