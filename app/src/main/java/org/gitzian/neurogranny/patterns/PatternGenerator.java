package org.gitzian.neurogranny.patterns;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Draws deterministic visual cues with strong silhouettes and low visual noise.
 *
 * <p>Each cue combines a small background anchor with one dominant glyph. The
 * drawing space is normalized to a square and then scaled by the caller.
 */
public final class PatternGenerator {
    public static final int VERSION = PatternDesign.FORMAT_VERSION;

    private static final float DESIGN_SIZE = 1_000.0f;
    private static final float CENTER = DESIGN_SIZE / 2.0f;
    private static final float[][] CORNERS = {
            {140.0f, 175.0f},
            {790.0f, 155.0f},
            {175.0f, 790.0f},
            {790.0f, 785.0f}
    };

    // Background, ink, accent, secondary.
    private static final int[][] PALETTES = {
            {0xfff6f2e9, 0xff292538, 0xff7654ba, 0xffe4a55c},
            {0xfff1f7f7, 0xff203a48, 0xffd45b58, 0xff6c9ba6},
            {0xff22253b, 0xfff5eedb, 0xfff5ab52, 0xff72b9b6},
            {0xfff6f0f1, 0xff332735, 0xffcd6d8b, 0xff569da2},
            {0xfff2f4eb, 0xff254137, 0xffc77d4c, 0xff8d77bc},
            {0xff202b2d, 0xfff6f0e4, 0xffdda76a, 0xff9c86d0},
            {0xfff8f5f0, 0xff232d37, 0xff498aae, 0xffd78b62},
            {0xffeceaf6, 0xff292844, 0xffb56c5b, 0xff718da2},
            {0xfff1f3f8, 0xff252e48, 0xff8265b6, 0xffd1aa61},
            {0xfff9f2e9, 0xff343137, 0xff4e9a8b, 0xffce7682}
    };

    private PatternGenerator() {
    }

    public static void render(Canvas canvas, long seed, float left, float top, float size) {
        if (size <= 0.0f) {
            return;
        }

        PatternDesign design = PatternDesign.fromSeed(seed);
        int[] colors = PALETTES[design.paletteIndex];
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.save();
        canvas.clipRect(left, top, left + size, top + size);
        canvas.translate(left, top);
        canvas.scale(size / DESIGN_SIZE, size / DESIGN_SIZE);

        fill(paint, colors[0]);
        canvas.drawRect(0.0f, 0.0f, DESIGN_SIZE, DESIGN_SIZE, paint);

        drawBackgroundShape(canvas, paint, design, colors[3]);
        drawAnchorDot(canvas, paint, design, colors[2]);

        canvas.save();
        canvas.translate(design.offsetX, design.offsetY);
        canvas.scale(design.glyphScale, design.glyphScale, CENTER, CENTER);
        canvas.rotate(design.rotationDegrees, CENTER, CENTER);
        if (design.mirrored) {
            canvas.scale(-1.0f, 1.0f, CENTER, CENTER);
        }

        drawGlyph(canvas, paint, design.archetype, colors[1], colors[2], colors[3]);
        canvas.restore();
        canvas.restore();
    }

    private static void drawBackgroundShape(Canvas canvas, Paint paint,
            PatternDesign design, int color) {
        float[] point = CORNERS[design.backgroundCorner];
        float x = point[0];
        float y = point[1];
        fill(paint, color);

        switch (design.backgroundMode) {
            case CIRCLE:
                canvas.drawCircle(x, y, 120.0f, paint);
                break;
            case TRIANGLE:
                Path triangle = new Path();
                triangle.moveTo(x - 110.0f, y - 70.0f);
                triangle.lineTo(x + 120.0f, y - 50.0f);
                triangle.lineTo(x + 40.0f, y + 145.0f);
                triangle.close();
                canvas.drawPath(triangle, paint);
                break;
            case ROUNDED_BLOCK:
                RectF rounded = new RectF(x - 120.0f, y - 80.0f, x + 95.0f, y + 85.0f);
                canvas.drawRoundRect(rounded, 28.0f, 28.0f, paint);
                break;
            case ARC:
                stroke(paint, color, 42.0f);
                canvas.drawArc(new RectF(x - 130.0f, y - 130.0f, x + 130.0f, y + 130.0f),
                        0.0f, 290.0f, false, paint);
                break;
            case SHARD:
                Path shard = new Path();
                shard.moveTo(x - 115.0f, y + 40.0f);
                shard.lineTo(x + 70.0f, y - 120.0f);
                shard.lineTo(x + 118.0f, y - 70.0f);
                shard.lineTo(x - 65.0f, y + 90.0f);
                shard.close();
                fill(paint, color);
                canvas.drawPath(shard, paint);
                break;
            default:
                throw new IllegalStateException("Unknown background mode: " + design.backgroundMode);
        }
    }

    private static void drawAnchorDot(Canvas canvas, Paint paint,
            PatternDesign design, int color) {
        float[] point = CORNERS[design.anchorCorner];
        fill(paint, color);
        canvas.drawCircle(point[0], point[1], design.accentRadius, paint);
    }

    private static void drawGlyph(Canvas canvas, Paint paint,
            PatternDesign.Archetype archetype, int ink, int accent, int secondary) {
        switch (archetype) {
            case FORKED_RUNE:
                drawForkedRune(canvas, paint, ink, accent, secondary);
                break;
            case OPEN_RINGS:
                drawOpenRings(canvas, paint, ink, accent, secondary);
                break;
            case SHARD:
                drawShard(canvas, paint, ink, accent, secondary);
                break;
            case TOTEM:
                drawTotem(canvas, paint, ink, accent, secondary);
                break;
            case DOUBLE_CHEVRON:
                drawDoubleChevron(canvas, paint, ink, accent, secondary);
                break;
            case ORBIT:
                drawOrbit(canvas, paint, ink, accent, secondary);
                break;
            case CURVED_WAVE:
                drawCurvedWave(canvas, paint, ink, accent, secondary);
                break;
            case LADDER:
                drawLadder(canvas, paint, ink, accent, secondary);
                break;
            case HOOK:
                drawHook(canvas, paint, ink, accent, secondary);
                break;
            case COMPASS:
                drawCompass(canvas, paint, ink, accent, secondary);
                break;
            default:
                throw new IllegalStateException("Unknown archetype: " + archetype);
        }
    }

    private static void drawForkedRune(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 40.0f);
        Path main = new Path();
        main.moveTo(250.0f, 720.0f);
        main.lineTo(310.0f, 560.0f);
        main.lineTo(275.0f, 365.0f);
        main.lineTo(415.0f, 230.0f);
        main.lineTo(520.0f, 365.0f);
        canvas.drawPath(main, paint);

        Path branch = new Path();
        branch.moveTo(310.0f, 560.0f);
        branch.lineTo(475.0f, 640.0f);
        branch.lineTo(605.0f, 510.0f);
        canvas.drawPath(branch, paint);

        fill(paint, accent);
        canvas.drawCircle(520.0f, 365.0f, 35.0f, paint);

        Path wedge = new Path();
        wedge.moveTo(195.0f, 690.0f);
        wedge.lineTo(275.0f, 720.0f);
        wedge.lineTo(232.0f, 785.0f);
        wedge.close();
        fill(paint, secondary);
        canvas.drawPath(wedge, paint);
    }

    private static void drawOpenRings(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 48.0f);
        canvas.drawArc(new RectF(210.0f, 205.0f, 790.0f, 785.0f), 25.0f, 275.0f, false, paint);

        strokeRound(paint, accent, 52.0f);
        canvas.drawArc(new RectF(345.0f, 340.0f, 655.0f, 650.0f), 195.0f, 300.0f, false, paint);

        strokeRound(paint, ink, 40.0f);
        Path keyway = new Path();
        keyway.moveTo(565.0f, 500.0f);
        keyway.lineTo(735.0f, 500.0f);
        keyway.lineTo(735.0f, 640.0f);
        canvas.drawPath(keyway, paint);

        fill(paint, secondary);
        canvas.drawCircle(735.0f, 640.0f, 33.0f, paint);
    }

    private static void drawShard(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        Path body = new Path();
        body.moveTo(485.0f, 150.0f);
        body.lineTo(700.0f, 290.0f);
        body.lineTo(620.0f, 435.0f);
        body.lineTo(760.0f, 575.0f);
        body.lineTo(470.0f, 790.0f);
        body.lineTo(410.0f, 555.0f);
        body.lineTo(250.0f, 445.0f);
        body.lineTo(385.0f, 325.0f);
        body.close();
        fill(paint, ink);
        canvas.drawPath(body, paint);

        Path core = new Path();
        core.moveTo(495.0f, 290.0f);
        core.lineTo(585.0f, 340.0f);
        core.lineTo(500.0f, 465.0f);
        core.lineTo(430.0f, 390.0f);
        core.close();
        fill(paint, accent);
        canvas.drawPath(core, paint);

        fill(paint, secondary);
        canvas.drawCircle(515.0f, 640.0f, 40.0f, paint);
    }

    private static void drawTotem(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 44.0f);
        canvas.drawCircle(500.0f, 280.0f, 130.0f, paint);

        strokeRound(paint, accent, 34.0f);
        canvas.drawLine(370.0f, 280.0f, 630.0f, 280.0f, paint);

        strokeRound(paint, ink, 54.0f);
        canvas.drawLine(500.0f, 410.0f, 500.0f, 650.0f, paint);

        fill(paint, ink);
        canvas.drawRoundRect(new RectF(325.0f, 640.0f, 675.0f, 740.0f), 40.0f, 40.0f, paint);

        fill(paint, accent);
        canvas.drawCircle(500.0f, 690.0f, 38.0f, paint);
        fill(paint, secondary);
        canvas.drawCircle(500.0f, 690.0f, 14.0f, paint);
    }

    private static void drawDoubleChevron(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        Path left = new Path();
        left.moveTo(180.0f, 460.0f);
        left.lineTo(360.0f, 230.0f);
        left.lineTo(505.0f, 380.0f);
        left.lineTo(400.0f, 520.0f);
        left.lineTo(320.0f, 435.0f);
        left.lineTo(240.0f, 545.0f);
        left.close();
        fill(paint, ink);
        canvas.drawPath(left, paint);

        Path right = new Path();
        right.moveTo(520.0f, 580.0f);
        right.lineTo(680.0f, 380.0f);
        right.lineTo(810.0f, 500.0f);
        right.lineTo(655.0f, 725.0f);
        right.lineTo(565.0f, 635.0f);
        right.lineTo(630.0f, 585.0f);
        right.close();
        fill(paint, accent);
        canvas.drawPath(right, paint);

        strokeRound(paint, secondary, 32.0f);
        canvas.drawLine(360.0f, 665.0f, 470.0f, 665.0f, paint);
    }

    private static void drawOrbit(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 46.0f);
        canvas.drawArc(new RectF(220.0f, 315.0f, 780.0f, 725.0f), 50.0f, 280.0f, false, paint);

        strokeRound(paint, accent, 40.0f);
        canvas.drawArc(new RectF(350.0f, 195.0f, 665.0f, 825.0f), 140.0f, 275.0f, false, paint);

        fill(paint, ink);
        canvas.drawCircle(395.0f, 430.0f, 56.0f, paint);
        fill(paint, secondary);
        canvas.drawCircle(655.0f, 620.0f, 52.0f, paint);
    }

    private static void drawCurvedWave(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 46.0f);
        Path wave = new Path();
        wave.moveTo(210.0f, 455.0f);
        wave.cubicTo(315.0f, 310.0f, 385.0f, 675.0f, 500.0f, 515.0f);
        wave.cubicTo(620.0f, 350.0f, 700.0f, 640.0f, 800.0f, 405.0f);
        canvas.drawPath(wave, paint);

        strokeRound(paint, accent, 34.0f);
        canvas.drawLine(360.0f, 650.0f, 360.0f, 795.0f, paint);

        fill(paint, secondary);
        canvas.drawCircle(660.0f, 330.0f, 42.0f, paint);
    }

    private static void drawLadder(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 48.0f);
        canvas.drawLine(345.0f, 760.0f, 430.0f, 220.0f, paint);
        canvas.drawLine(590.0f, 830.0f, 690.0f, 300.0f, paint);

        strokeRound(paint, ink, 34.0f);
        canvas.drawLine(410.0f, 355.0f, 675.0f, 390.0f, paint);
        strokeRound(paint, accent, 34.0f);
        canvas.drawLine(380.0f, 515.0f, 620.0f, 540.0f, paint);
        strokeRound(paint, ink, 34.0f);
        canvas.drawLine(355.0f, 680.0f, 580.0f, 710.0f, paint);

        fill(paint, secondary);
        canvas.drawCircle(430.0f, 220.0f, 33.0f, paint);
    }

    private static void drawHook(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        strokeRound(paint, ink, 56.0f);
        canvas.drawArc(new RectF(280.0f, 185.0f, 770.0f, 675.0f), 5.0f, 265.0f, false, paint);

        Path tail = new Path();
        tail.moveTo(535.0f, 470.0f);
        tail.lineTo(355.0f, 655.0f);
        tail.lineTo(460.0f, 795.0f);
        canvas.drawPath(tail, paint);

        Path flag = new Path();
        flag.moveTo(585.0f, 185.0f);
        flag.lineTo(755.0f, 245.0f);
        flag.lineTo(660.0f, 360.0f);
        flag.close();
        fill(paint, accent);
        canvas.drawPath(flag, paint);

        fill(paint, secondary);
        canvas.drawCircle(455.0f, 795.0f, 40.0f, paint);
    }

    private static void drawCompass(Canvas canvas, Paint paint, int ink, int accent, int secondary) {
        drawCompassArm(canvas, paint, CENTER, CENTER, 235.0f, -1.70f, ink);
        drawCompassArm(canvas, paint, CENTER, CENTER, 205.0f, -0.57f, ink);
        drawCompassArm(canvas, paint, CENTER, CENTER, 205.0f, 0.58f, accent);
        drawCompassArm(canvas, paint, CENTER, CENTER, 235.0f, 1.73f, accent);

        fill(paint, secondary);
        canvas.drawCircle(CENTER, CENTER, 78.0f, paint);
        fill(paint, ink);
        canvas.drawCircle(CENTER, CENTER, 28.0f, paint);
    }

    private static void drawCompassArm(Canvas canvas, Paint paint,
            float centerX, float centerY, float length, float angleRadians, int color) {
        float tipX = polarX(centerX, length, angleRadians);
        float tipY = polarY(centerY, length, angleRadians);
        float leftX = polarX(centerX, 92.0f, angleRadians - 0.22f);
        float leftY = polarY(centerY, 92.0f, angleRadians - 0.22f);
        float rightX = polarX(centerX, 92.0f, angleRadians + 0.22f);
        float rightY = polarY(centerY, 92.0f, angleRadians + 0.22f);

        Path arm = new Path();
        arm.moveTo(leftX, leftY);
        arm.lineTo(tipX, tipY);
        arm.lineTo(rightX, rightY);
        arm.close();
        fill(paint, color);
        canvas.drawPath(arm, paint);
    }

    private static float polarX(float centerX, float radius, float angleRadians) {
        return (float) (centerX + radius * Math.cos(angleRadians));
    }

    private static float polarY(float centerY, float radius, float angleRadians) {
        return (float) (centerY + radius * Math.sin(angleRadians));
    }

    private static void fill(Paint paint, int color) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    private static void stroke(Paint paint, int color, float strokeWidth) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(color);
    }

    private static void strokeRound(Paint paint, int color, float strokeWidth) {
        stroke(paint, color, strokeWidth);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }
}
