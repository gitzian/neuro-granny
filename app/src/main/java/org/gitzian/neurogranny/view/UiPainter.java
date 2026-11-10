package org.gitzian.neurogranny.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.android.AppLanguage;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ReviewEvent;
import org.gitzian.neurogranny.model.ReviewSettings;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;

/**
 * Paints the GL-backed UI using density-independent coordinates.
 *
 * <p>Header, scrolling content and footer are three separate drawing regions.
 * Hit targets are clipped to their region, so an invisible chart cannot intercept
 * a footer button, even on a short landscape display.
 */
public final class UiPainter {
    public static final int BG = 0xff08080b;
    public static final int TEXT = 0xffe8e5eb;
    public static final int MUTED = 0xffa4a0a9;

    private static final int OUTLINE = 0xff38303d;
    private static final int PURPLE = 0xff493357;
    private static final int PURPLE_SOFT = 0xff27202d;
    // Screen coordinates and padding are in dp; font sizes are in sp.
    private static final float CONTENT_TOP_DP = 66.0f;
    private static final float CONTENT_PADDING_DP = 12.0f;
    private static final float MAX_CONTENT_WIDTH_DP = 520.0f;
    private static final float MIN_CONTENT_WIDTH_DP = 1.0f;
    private static final float SIDE_INSET_DP = 16.0f;
    private static final float CONTENT_SEPARATOR_OFFSET_DP = 4.0f;
    private static final float CONTENT_VIEWPORT_MIN_HEIGHT_DP = 1.0f;
    private static final float FOOTER_ROW_STEP_DP = 52.0f;
    private static final float FOOTER_BOTTOM_MARGIN_DP = 24.0f;
    private static final float FOOTER_TOP_CLEARANCE_DP = 12.0f;
    private static final float PROMPT_OVERLAY_RESERVE_DP = 150.0f;

    // Header.
    private static final float HEADER_VERSION_SIZE_SP = 11.0f;
    private static final float HEADER_APP_NAME_SIZE_SP = 19.0f;
    private static final float HEADER_MIN_NAME_WIDTH_DP = 70.0f;
    private static final float HEADER_NAME_VERSION_GAP_DP = 16.0f;
    private static final float HEADER_CLIP_TOP_DP = 12.0f;
    private static final float HEADER_CLIP_BOTTOM_DP = 51.0f;
    private static final float HEADER_NAME_BASELINE_DP = 40.0f;
    private static final float HEADER_VERSION_BASELINE_DP = 39.0f;
    private static final float HEADER_SEPARATOR_Y_DP = 54.0f;

    // Home list.
    private static final float HOME_TOP_DP = 74.0f;
    private static final float HOME_ITEM_TEXT_INSET_DP = 10.0f;
    private static final float HOME_ITEM_TITLE_SIZE_SP = 15.0f;
    private static final float HOME_ITEM_STATUS_SIZE_SP = 12.0f;
    private static final float HOME_ITEM_MIN_HEIGHT_DP = 62.0f;
    private static final float HOME_ITEM_TITLE_STATUS_GAP_DP = 5.0f;
    private static final float HOME_ITEM_BOTTOM_PADDING_DP = 12.0f;
    private static final float HOME_ITEM_GAP_DP = 2.0f;
    private static final float HOME_ITEM_SEPARATOR_INSET_DP = 1.0f;
    private static final float HOME_EMPTY_TEXT_INSET_DP = 8.0f;
    private static final float HOME_EMPTY_BOTTOM_GAP_DP = 12.0f;

    // Details, cue, review, and history.
    private static final float CONTENT_SECTION_TOP_DP = 78.0f;
    private static final float DETAILS_TITLE_GAP_DP = 12.0f;
    private static final float DETAILS_MODE_GAP_DP = 10.0f;
    private static final float DETAILS_SCHEDULE_GAP_DP = 6.0f;
    private static final float DETAILS_INTERVAL_GAP_DP = 12.0f;
    private static final float DETAILS_ACTION_MIN_HEIGHT_DP = 34.0f;
    private static final float DETAILS_ACTION_TEXT_INSET_DP = 6.0f;
    private static final float DETAILS_ACTION_VERTICAL_PADDING_DP = 6.0f;
    private static final float DETAILS_ACTION_BOTTOM_GAP_DP = 9.0f;
    private static final float DETAILS_REVEAL_MIN_HEIGHT_DP = 35.0f;
    private static final float DETAILS_REVEAL_BOTTOM_GAP_DP = 10.0f;
    private static final float DETAILS_HISTORY_TITLE_SIZE_SP = 12.0f;
    private static final float DETAILS_HISTORY_LABEL_GAP_DP = 7.0f;
    private static final float DETAILS_CHART_HEIGHT_DP = 145.0f;
    private static final float DETAILS_CHART_GAP_DP = 9.0f;
    private static final float DETAILS_HISTORY_HINT_SIZE_SP = 11.0f;
    private static final float DETAILS_HISTORY_HINT_GAP_DP = 12.0f;
    private static final float CUE_TITLE_SIZE_SP = 16.0f;
    private static final float CUE_TITLE_GAP_DP = 14.0f;
    private static final float CUE_MAX_SIZE_DP = 340.0f;
    private static final float CUE_BOTTOM_GAP_DP = 12.0f;
    private static final float PROMPT_CONTENT_TOP_DP = 76.0f;
    private static final float PROMPT_TITLE_GAP_DP = 13.0f;
    private static final float PROMPT_MAX_CUE_SIZE_DP = 300.0f;
    private static final float PROMPT_CUE_GAP_DP = 13.0f;
    private static final float PROMPT_HINT_SIZE_SP = 12.0f;
    private static final float PROMPT_HINT_GAP_DP = 7.0f;
    private static final float PROMPT_ATTEMPTS_GAP_DP = 9.0f;
    private static final float HISTORY_TITLE_GAP_DP = 10.0f;
    private static final float HISTORY_ROW_INSET_DP = 10.0f;
    private static final float HISTORY_ROW_TEXT_TOP_DP = 8.0f;
    private static final float HISTORY_ROW_MIN_HEIGHT_DP = 42.0f;
    private static final float HISTORY_ROW_BOTTOM_GAP_DP = 16.0f;
    private static final float HISTORY_ROW_TEXT_SIZE_SP = 13.0f;
    private static final float HISTORY_CHART_TITLE_GAP_DP = 7.0f;
    private static final float HISTORY_CHART_HEIGHT_DP = 280.0f;
    private static final float HISTORY_CHART_BOTTOM_GAP_DP = 12.0f;
    private static final float HISTORY_SUMMARY_TEXT_SIZE_SP = 12.0f;
    private static final float HISTORY_SUMMARY_BOTTOM_GAP_DP = 12.0f;

    // Settings and support button.
    private static final float SETTINGS_TITLE_GAP_DP = 10.0f;
    private static final float SETTINGS_LANGUAGE_INSET_DP = 10.0f;
    private static final float SETTINGS_LANGUAGE_SIZE_SP = 14.0f;
    private static final float SETTINGS_SELECTION_SIZE_SP = 13.0f;
    private static final float SETTINGS_LANGUAGE_MIN_HEIGHT_DP = 70.0f;
    private static final float SETTINGS_LANGUAGE_LABEL_TOP_DP = 6.0f;
    private static final float SETTINGS_SELECTION_LABEL_GAP_DP = 4.0f;
    private static final float SETTINGS_LANGUAGE_VERTICAL_PADDING_DP = 18.0f;
    private static final float SETTINGS_LANGUAGE_BOTTOM_GAP_DP = 17.0f;
    private static final float SETTINGS_EXPLANATION_GAP_DP = 9.0f;
    private static final float SETTINGS_SUPPORT_TOP_GAP_DP = 18.0f;
    private static final float SETTINGS_SUPPORT_BOTTOM_GAP_DP = 20.0f;
    private static final float SUPPORT_LEADING_PADDING_DP = 14.0f;
    private static final float SUPPORT_TRAILING_PADDING_DP = 14.0f;
    private static final float SUPPORT_ICON_SLOT_WIDTH_DP = 28.0f;
    private static final float SUPPORT_ICON_LABEL_GAP_DP = 10.0f;
    private static final float SUPPORT_BUTTON_EXTRA_HEIGHT_DP = 16.0f;
    private static final float SUPPORT_ICON_LEFT_OFFSET_DP = 1.0f;
    private static final float SUPPORT_ICON_CENTER_Y_OFFSET_DP = 13.0f;
    private static final float SUPPORT_CUP_STROKE_WIDTH_DP = 1.8f;
    private static final float SUPPORT_CUP_TOP_DP = 5.0f;
    private static final float SUPPORT_CUP_WIDTH_DP = 16.0f;
    private static final float SUPPORT_CUP_BOTTOM_DP = 19.0f;
    private static final float SUPPORT_HANDLE_LEFT_DP = 12.0f;
    private static final float SUPPORT_HANDLE_TOP_DP = 7.0f;
    private static final float SUPPORT_HANDLE_RIGHT_DP = 25.0f;
    private static final float SUPPORT_HANDLE_BOTTOM_DP = 17.0f;
    private static final float SUPPORT_HANDLE_START_ANGLE_DEG = -90.0f;
    private static final float SUPPORT_HANDLE_SWEEP_ANGLE_DEG = 180.0f;
    private static final float SUPPORT_SAUCER_LEFT_DP = -2.0f;
    private static final float SUPPORT_SAUCER_RIGHT_DP = 20.0f;
    private static final float SUPPORT_SAUCER_TOP_DP = 22.0f;

    // Footer buttons.
    private static final float BUTTON_HEIGHT_DP = 44.0f;
    private static final float BUTTON_CORNER_RADIUS_DP = 5.0f;
    private static final float BUTTON_TEXT_HORIZONTAL_PADDING_DP = 8.0f;
    private static final float BUTTON_TEXT_VERTICAL_PADDING_DP = 3.0f;
    private static final float BUTTON_MIN_TEXT_SIZE_SP = 11.0f;
    private static final float BUTTON_TEXT_SIZE_STEP_SP = 0.5f;
    private static final float FOOTER_BUTTON_GAP_DP = 8.0f;
    private static final float BORDER_STROKE_WIDTH_DP = 1.0f;
    private static final int TEXT_BLOCK_CACHE_SIZE = 192;
    private static final int BUTTON_TEXT_BLOCK_CACHE_SIZE = 64;
    private static final long MILLIS_PER_SECOND = 1_000L;

    public static final class Hit {
        public final RectF rect;
        public final UiAction action;

        private Hit(RectF rect, UiAction action) {
            this.rect = rect;
            this.action = action;
        }
    }

    public static final class UiLayout {
        public final List<Hit> footerHits;
        public final List<Hit> contentHits;
        public final RectF contentViewport;
        public final int maxScroll;

        private UiLayout(List<Hit> footerHits, List<Hit> contentHits,
                RectF contentViewport, int maxScroll) {
            this.footerHits = Collections.unmodifiableList(new ArrayList<>(footerHits));
            this.contentHits = Collections.unmodifiableList(new ArrayList<>(contentHits));
            this.contentViewport = new RectF(contentViewport);
            this.maxScroll = maxScroll;
        }

        /** Footer actions take precedence if a region ever touches an edge. */
        public UiAction actionAt(float x, float y) {
            for (Hit hit : footerHits) {
                if (hit.rect.contains(x, y)) {
                    return hit.action;
                }
            }
            if (!contentViewport.contains(x, y)) {
                return null;
            }
            for (Hit hit : contentHits) {
                if (hit.rect.contains(x, y)) {
                    return hit.action;
                }
            }
            return null;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LruCache<String, TextBlock> textBlocks =
            new LruCache<>(TEXT_BLOCK_CACHE_SIZE);
    private final LruCache<String, TextBlock> buttonTextBlocks =
            new LruCache<>(BUTTON_TEXT_BLOCK_CACHE_SIZE);
    private final UiTypography typography;
    private final List<Hit> footerHits = new ArrayList<>();
    private final List<Hit> contentHits = new ArrayList<>();
    private final Context context;
    private final TimeFormatter timeFormatter;
    private final ReviewChartRenderer chartRenderer;
    private final String appVersion;

    private Canvas canvas;
    private float screenHeight;
    private float left;
    private float right;
    private float contentScroll;
    private RectF viewport;

    public UiPainter(Context context) {
        this.context = context;
        this.typography = new UiTypography(context);
        this.timeFormatter = new TimeFormatter(context);
        this.chartRenderer = new ReviewChartRenderer(context, typography);
        this.appVersion = AppVersion.display(context);
    }

    public UiLayout paint(Canvas target, float widthDp, float heightDp,
            UiScene scene) {
        canvas = target;
        screenHeight = heightDp;
        contentScroll = Math.max(0, scene.listScroll);
        footerHits.clear();
        contentHits.clear();

        float contentWidth = Math.min(MAX_CONTENT_WIDTH_DP,
                Math.max(MIN_CONTENT_WIDTH_DP, widthDp - 2.0f * SIDE_INSET_DP));
        left = (widthDp - contentWidth) / 2.0f;
        right = left + contentWidth;

        int footerRows = footerRows(scene);
        float footerTop = heightDp - FOOTER_BOTTOM_MARGIN_DP
                - footerRows * FOOTER_ROW_STEP_DP - FOOTER_TOP_CLEARANCE_DP;
        if (scene.page == UiScene.Page.PROMPT) {
            // A native password field and its actions occupy the bottom overlay.
            footerTop = heightDp - PROMPT_OVERLAY_RESERVE_DP;
        }
        viewport = new RectF(left, CONTENT_TOP_DP, right,
                Math.max(CONTENT_TOP_DP + CONTENT_VIEWPORT_MIN_HEIGHT_DP, footerTop));

        canvas.drawColor(BG);
        drawHeader();

        // The clip is applied BEFORE translation; the footer is drawn afterwards.
        canvas.save();
        canvas.clipRect(viewport);
        canvas.translate(0.0f, -contentScroll);
        float contentBottom = drawContent(scene);
        canvas.restore();

        drawLine(left, viewport.bottom + CONTENT_SEPARATOR_OFFSET_DP,
                right, viewport.bottom + CONTENT_SEPARATOR_OFFSET_DP, OUTLINE);
        drawFooter(scene);
        int maxScroll = Math.max(0, (int) Math.ceil(
                contentBottom + CONTENT_PADDING_DP - viewport.bottom));
        return new UiLayout(footerHits, contentHits, viewport, maxScroll);
    }

    private int footerRows(UiScene scene) {
        switch (scene.page) {
            case EDITOR:
                return scene.current == null ? 1 : 2;
            case DETAILS:
            case SETTINGS:
                return 2;
            case PROMPT:
                return 0;
            default:
                return 1;
        }
    }

    private void drawHeader() {
        paint.setTypeface(typography.normal());
        paint.setTextSize(typography.spToDp(HEADER_VERSION_SIZE_SP));
        float versionWidth = paint.measureText(appVersion);
        float labelWidth = Math.max(HEADER_MIN_NAME_WIDTH_DP,
                right - left - versionWidth - HEADER_NAME_VERSION_GAP_DP);

        String appTitle = context.getString(R.string.app_name);
        boolean isDebug = (context.getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        if (isDebug) {
            appTitle += " (dev)";
        }

        canvas.save();
        canvas.clipRect(left, HEADER_CLIP_TOP_DP,
                left + labelWidth, HEADER_CLIP_BOTTOM_DP);
        drawText(appTitle, left,
                HEADER_NAME_BASELINE_DP, HEADER_APP_NAME_SIZE_SP, TEXT, true);
        canvas.restore();

        drawText(appVersion, right - versionWidth,
                HEADER_VERSION_BASELINE_DP, HEADER_VERSION_SIZE_SP, MUTED, false);
        drawLine(left, HEADER_SEPARATOR_Y_DP, right, HEADER_SEPARATOR_Y_DP, OUTLINE);
    }

    private float drawContent(UiScene scene) {
        switch (scene.page) {
            case HOME:
                return drawHome(scene);
            case DETAILS:
                return drawDetails(scene);
            case EDITOR:
                // Native scrollable form occupies this viewport.
                return CONTENT_TOP_DP;
            case CUE:
                return drawCue(scene);
            case PROMPT:
                return drawPrompt(scene);
            case HISTORY:
                return drawHistory(scene);
            case SETTINGS:
                return drawSettings();
            default:
                return CONTENT_TOP_DP;
        }
    }

    private float drawHome(UiScene scene) {
        VerticalStack stack = new VerticalStack(left, HOME_TOP_DP, right - left);
        long now = System.currentTimeMillis();
        float width = right - left - 2.0f * HOME_ITEM_TEXT_INSET_DP;
        for (Memory memory : scene.memories) {
            TextBlock name = block(memory.title, width,
                    HOME_ITEM_TITLE_SIZE_SP, TEXT, false);
            boolean due = memory.due || memory.nextDue <= now;
            String statusText = due ? context.getString(R.string.due_now)
                    : context.getString(R.string.next_status,
                            timeFormatter.remaining((memory.nextDue - now + MILLIS_PER_SECOND - 1L)
                                    / MILLIS_PER_SECOND));
            TextBlock status = block(statusText, width,
                    HOME_ITEM_STATUS_SIZE_SP, MUTED, false);
            float rowHeight = Math.max(HOME_ITEM_MIN_HEIGHT_DP, HOME_ITEM_TEXT_INSET_DP + name.height()
                    + HOME_ITEM_TITLE_STATUS_GAP_DP + status.height() + HOME_ITEM_BOTTOM_PADDING_DP);
            RectF row = stack.place(rowHeight, HOME_ITEM_GAP_DP);
            name.draw(canvas, row.left + HOME_ITEM_TEXT_INSET_DP,
                    row.top + HOME_ITEM_TEXT_INSET_DP);
            status.draw(canvas, row.left + HOME_ITEM_TEXT_INSET_DP, row.top + HOME_ITEM_TEXT_INSET_DP
                    + HOME_ITEM_TITLE_STATUS_GAP_DP + name.height());
            drawLine(left, row.bottom - HOME_ITEM_SEPARATOR_INSET_DP,
                    right, row.bottom - HOME_ITEM_SEPARATOR_INSET_DP, OUTLINE);
            addContentHit(row.left, row.top, row.width(), row.height(),
                    UiAction.selectMemory(memory.id));
        }
        if (scene.memories.isEmpty()) {
            TextBlock empty = block(context.getString(R.string.no_entries),
                    width, UiTypography.BODY_SP, MUTED, false);
            RectF bounds = stack.place(empty.height(), HOME_EMPTY_BOTTOM_GAP_DP);
            empty.draw(canvas, bounds.left + HOME_EMPTY_TEXT_INSET_DP, bounds.top);
        }
        return stack.bottom();
    }

    private float drawDetails(UiScene scene) {
        Memory memory = scene.current;
        if (memory == null) {
            return HOME_TOP_DP;
        }

        VerticalStack stack = new VerticalStack(left, CONTENT_SECTION_TOP_DP, right - left);
        drawStackText(stack, memory.title, UiTypography.TITLE_SP, TEXT, true, DETAILS_TITLE_GAP_DP);
        drawStackText(stack, context.getString(memory.mode == SecretMode.VERIFY_ONLY
                ? R.string.verify_only : R.string.recoverable),
                UiTypography.BODY_SP, MUTED, false, DETAILS_MODE_GAP_DP);

        int scheduleId = memory.scheduleMode == ScheduleMode.FIXED ? R.string.schedule_fixed
                : memory.scheduleMode == ScheduleMode.AUTO_LEARNER ? R.string.schedule_auto
                : R.string.schedule_ask_next;
        drawStackText(stack, context.getString(R.string.schedule_value,
                context.getString(scheduleId)),
                UiTypography.BODY_SP, MUTED, false, DETAILS_SCHEDULE_GAP_DP);
        String interval = memory.scheduleMode == ScheduleMode.AUTO_LEARNER
                ? context.getString(R.string.range_value,
                        timeFormatter.interval(memory.minSeconds),
                        timeFormatter.interval(memory.maxSeconds))
                : context.getString(R.string.interval_value,
                        timeFormatter.interval(memory.fixedSeconds));
        drawStackText(stack, interval, UiTypography.BODY_SP, MUTED, false, DETAILS_INTERVAL_GAP_DP);

        boolean due = memory.due || memory.nextDue <= System.currentTimeMillis();
        String dueText = due ? context.getString(R.string.review_due_tap)
                : context.getString(R.string.next_status, timeFormatter.dateTime(memory.nextDue));
        TextBlock dueBlock = block(dueText, right - left, UiTypography.BODY_SP, MUTED, false);
        RectF dueArea = stack.place(Math.max(DETAILS_ACTION_MIN_HEIGHT_DP, dueBlock.height()
                + 2.0f * DETAILS_ACTION_VERTICAL_PADDING_DP), DETAILS_ACTION_BOTTOM_GAP_DP);
        dueBlock.draw(canvas, dueArea.left, dueArea.top + DETAILS_ACTION_TEXT_INSET_DP);
        if (due) {
            addContentHit(dueArea.left, dueArea.top, dueArea.width(), dueArea.height(),
                    UiAction.of(UiAction.Type.OPEN_REVIEW));
        }

        drawStackText(stack, context.getString(R.string.successful_reviews, memory.successes),
                UiTypography.BODY_SP, MUTED, false, DETAILS_ACTION_BOTTOM_GAP_DP);
        if (memory.mode == SecretMode.RECOVERABLE) {
            TextBlock reveal = block(context.getString(R.string.reveal_stored_password),
                    right - left, UiTypography.BODY_SP, MUTED, false);
            RectF revealArea = stack.place(Math.max(DETAILS_REVEAL_MIN_HEIGHT_DP, reveal.height()
                    + 2.0f * DETAILS_ACTION_VERTICAL_PADDING_DP), DETAILS_REVEAL_BOTTOM_GAP_DP);
            reveal.draw(canvas, revealArea.left, revealArea.top + DETAILS_ACTION_TEXT_INSET_DP);
            addContentHit(revealArea.left, revealArea.top,
                    revealArea.width(), revealArea.height(),
                    UiAction.of(UiAction.Type.REVEAL_SECRET));
        }

        drawStackText(stack, context.getString(R.string.recall_history_last_days,
                ReviewSettings.HISTORY_WINDOW_DAYS), DETAILS_HISTORY_TITLE_SIZE_SP, MUTED,
                false, DETAILS_HISTORY_LABEL_GAP_DP);
        RectF chart = stack.place(DETAILS_CHART_HEIGHT_DP, DETAILS_CHART_GAP_DP);
        drawChart(scene.history, chart.left, chart.top, chart.width(), chart.height(),
                false, scene.historyFrom, scene.historyTo);
        addContentHit(chart.left, chart.top, chart.width(), chart.height(),
                UiAction.of(UiAction.Type.OPEN_HISTORY));
        drawStackText(stack, context.getString(R.string.tap_chart_history),
                DETAILS_HISTORY_HINT_SIZE_SP, MUTED, false, DETAILS_HISTORY_HINT_GAP_DP);
        return stack.bottom();
    }

    private float drawCue(UiScene scene) {
        if (scene.current == null) {
            return HOME_TOP_DP;
        }
        VerticalStack stack = new VerticalStack(left, CONTENT_SECTION_TOP_DP, right - left);
        drawStackText(stack, scene.current.title, CUE_TITLE_SIZE_SP, TEXT, false, CUE_TITLE_GAP_DP);
        float size = Math.min(right - left, CUE_MAX_SIZE_DP);
        RectF slot = stack.place(size, CUE_BOTTOM_GAP_DP);
        CueRenderer.render(canvas, scene.current,
                slot.centerX() - size / 2.0f, slot.top, size);
        return stack.bottom();
    }

    private float drawPrompt(UiScene scene) {
        if (scene.current == null) {
            return HOME_TOP_DP;
        }
        VerticalStack stack = new VerticalStack(left, PROMPT_CONTENT_TOP_DP, right - left);
        drawStackText(stack, scene.current.title,
                UiTypography.TITLE_SP, TEXT, true, PROMPT_TITLE_GAP_DP);
        float size = Math.min(right - left, PROMPT_MAX_CUE_SIZE_DP);
        RectF slot = stack.place(size, PROMPT_CUE_GAP_DP);
        CueRenderer.render(canvas, scene.current,
                slot.centerX() - size / 2.0f, slot.top, size);
        drawStackText(stack, context.getString(R.string.enter_to_verify), PROMPT_HINT_SIZE_SP, MUTED,
                false, PROMPT_HINT_GAP_DP);
        if (scene.wrongAttempts > 0) {
            drawStackText(stack, context.getString(R.string.incorrect_attempts, scene.wrongAttempts),
                    PROMPT_HINT_SIZE_SP, MUTED, false, PROMPT_ATTEMPTS_GAP_DP);
        }
        return stack.bottom();
    }

    private float drawHistory(UiScene scene) {
        VerticalStack stack = new VerticalStack(left, CONTENT_SECTION_TOP_DP, right - left);
        drawStackText(stack, scene.current == null
                ? context.getString(R.string.history) : scene.current.title,
                UiTypography.TITLE_SP, TEXT, true, HISTORY_TITLE_GAP_DP);

        TextBlock from = block(context.getString(R.string.history_from,
                timeFormatter.dateTime(scene.historyFrom)),
                right - left - 2.0f * HISTORY_ROW_INSET_DP,
                HISTORY_ROW_TEXT_SIZE_SP, TEXT, false);
        RectF fromRow = stack.place(Math.max(HISTORY_ROW_MIN_HEIGHT_DP, from.height()
                + 2.0f * HISTORY_ROW_TEXT_TOP_DP), 0.0f);
        from.draw(canvas, fromRow.left + HISTORY_ROW_INSET_DP,
                fromRow.top + HISTORY_ROW_TEXT_TOP_DP);
        drawLine(left, fromRow.bottom, right, fromRow.bottom, OUTLINE);
        addContentHit(fromRow.left, fromRow.top, fromRow.width(), fromRow.height(),
                UiAction.of(UiAction.Type.HISTORY_START));

        TextBlock to = block(context.getString(R.string.history_to,
                timeFormatter.dateTime(scene.historyTo)),
                right - left - 2.0f * HISTORY_ROW_INSET_DP,
                HISTORY_ROW_TEXT_SIZE_SP, TEXT, false);
        RectF toRow = stack.place(Math.max(HISTORY_ROW_MIN_HEIGHT_DP, to.height()
                + 2.0f * HISTORY_ROW_TEXT_TOP_DP), HISTORY_ROW_BOTTOM_GAP_DP);
        to.draw(canvas, toRow.left + HISTORY_ROW_INSET_DP,
                toRow.top + HISTORY_ROW_TEXT_TOP_DP);
        drawLine(left, toRow.bottom, right, toRow.bottom, OUTLINE);
        addContentHit(toRow.left, toRow.top, toRow.width(), toRow.height(),
                UiAction.of(UiAction.Type.HISTORY_END));

        drawStackText(stack, context.getString(R.string.recall_history),
                HISTORY_ROW_TEXT_SIZE_SP, MUTED, false, HISTORY_CHART_TITLE_GAP_DP);
        RectF chart = stack.place(HISTORY_CHART_HEIGHT_DP, HISTORY_CHART_BOTTOM_GAP_DP);
        drawChart(scene.history, chart.left, chart.top, chart.width(), chart.height(),
                scene.curve, scene.historyFrom, scene.historyTo);
        int wrong = 0;
        int correct = 0;
        for (ReviewEvent event : scene.history) {
            if (event.correct) {
                correct++;
            } else {
                wrong++;
            }
        }
        drawStackText(stack, context.getString(R.string.correct_and_incorrect, correct, wrong),
                HISTORY_SUMMARY_TEXT_SIZE_SP, MUTED, false, HISTORY_SUMMARY_BOTTOM_GAP_DP);
        return stack.bottom();
    }

    private float drawSettings() {
        VerticalStack stack = new VerticalStack(left, CONTENT_SECTION_TOP_DP, right - left);
        drawStackText(stack, context.getString(R.string.settings),
                UiTypography.TITLE_SP, TEXT, true, SETTINGS_TITLE_GAP_DP);

        float inset = SETTINGS_LANGUAGE_INSET_DP;
        float textWidth = right - left - inset * 2.0f;
        TextBlock language = block(context.getString(R.string.language),
                textWidth, SETTINGS_LANGUAGE_SIZE_SP, TEXT, false);
        TextBlock selection = block(
                AppLanguage.selection(context) == AppLanguage.Choice.SYSTEM_DEFAULT
                        ? context.getString(R.string.system_default)
                        : AppLanguage.selection(context).nativeName(),
                textWidth, SETTINGS_SELECTION_SIZE_SP, MUTED, false);
        RectF languageRow = stack.place(Math.max(SETTINGS_LANGUAGE_MIN_HEIGHT_DP,
                language.height() + selection.height()
                        + SETTINGS_LANGUAGE_VERTICAL_PADDING_DP), SETTINGS_LANGUAGE_BOTTOM_GAP_DP);
        language.draw(canvas, languageRow.left + inset, languageRow.top + SETTINGS_LANGUAGE_LABEL_TOP_DP);
        selection.draw(canvas, languageRow.left + inset,
                languageRow.top + SETTINGS_LANGUAGE_LABEL_TOP_DP
                        + SETTINGS_SELECTION_LABEL_GAP_DP + language.height());
        drawLine(left, languageRow.bottom, right, languageRow.bottom, OUTLINE);
        addContentHit(languageRow.left, languageRow.top,
                languageRow.width(), languageRow.height(),
                UiAction.of(UiAction.Type.CHANGE_LANGUAGE));

        drawStackText(stack, context.getString(R.string.backups_readable),
                UiTypography.BODY_SP, MUTED, false, SETTINGS_EXPLANATION_GAP_DP);
        drawStackText(stack, context.getString(R.string.recoverable_plaintext),
                UiTypography.BODY_SP, MUTED, false, SETTINGS_EXPLANATION_GAP_DP);
        drawStackText(stack, context.getString(R.string.verify_only_hashed),
                UiTypography.BODY_SP, MUTED, false, SETTINGS_SUPPORT_TOP_GAP_DP);

        float width = right - left;
        HorizontalRow measure = new HorizontalRow(new RectF(left, 0, right, 1),
                SUPPORT_LEADING_PADDING_DP);
        measure.fixed(SUPPORT_ICON_SLOT_WIDTH_DP, SUPPORT_ICON_LABEL_GAP_DP);
        float labelWidth = measure.remainder(SUPPORT_TRAILING_PADDING_DP).width();
        TextBlock label = block(context.getString(R.string.support_button),
                labelWidth, UiTypography.BODY_SP, TEXT, false, Layout.Alignment.ALIGN_CENTER);
        RectF button = stack.place(
                Math.max(BUTTON_HEIGHT_DP, label.height() + SUPPORT_BUTTON_EXTRA_HEIGHT_DP),
                SETTINGS_SUPPORT_BOTTOM_GAP_DP);
        drawRectangle(button.left, button.top, width, button.height(),
                PURPLE, BUTTON_CORNER_RADIUS_DP);
        drawBorder(button.left, button.top, width, button.height());

        HorizontalRow row = new HorizontalRow(button, SUPPORT_LEADING_PADDING_DP);
        RectF iconSlot = row.fixed(SUPPORT_ICON_SLOT_WIDTH_DP, SUPPORT_ICON_LABEL_GAP_DP);
        RectF labelSlot = row.remainder(SUPPORT_TRAILING_PADDING_DP);
        drawCoffeeIcon(iconSlot.left + SUPPORT_ICON_LEFT_OFFSET_DP,
                iconSlot.centerY() - SUPPORT_ICON_CENTER_Y_OFFSET_DP);
        label.draw(canvas, labelSlot.left, labelSlot.centerY() - label.height() / 2.0f);
        addContentHit(button.left, button.top, button.width(), button.height(),
                UiAction.of(UiAction.Type.SUPPORT_PROJECT));
        return stack.bottom();
    }

    private void drawCoffeeIcon(float x, float y) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(SUPPORT_CUP_STROKE_WIDTH_DP);
        paint.setColor(TEXT);
        canvas.drawRect(x, y + SUPPORT_CUP_TOP_DP,
                x + SUPPORT_CUP_WIDTH_DP, y + SUPPORT_CUP_BOTTOM_DP, paint);
        canvas.drawArc(x + SUPPORT_HANDLE_LEFT_DP, y + SUPPORT_HANDLE_TOP_DP,
                x + SUPPORT_HANDLE_RIGHT_DP, y + SUPPORT_HANDLE_BOTTOM_DP,
                SUPPORT_HANDLE_START_ANGLE_DEG, SUPPORT_HANDLE_SWEEP_ANGLE_DEG, false, paint);
        canvas.drawLine(x + SUPPORT_SAUCER_LEFT_DP, y + SUPPORT_SAUCER_TOP_DP,
                x + SUPPORT_SAUCER_RIGHT_DP, y + SUPPORT_SAUCER_TOP_DP, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFooter(UiScene scene) {
        // Restore to an untransformed canvas before drawing non-scrolling actions.
        switch (scene.page) {
            case HOME:
                footerButton(context.getString(R.string.add_memory), UiAction.of(UiAction.Type.ADD_MEMORY), true, 0, 0, 2);
                footerButton(context.getString(R.string.settings), UiAction.of(UiAction.Type.OPEN_SETTINGS), false, 1, 0, 2);
                break;
            case EDITOR:
                if (scene.current != null) {
                    footerButton(context.getString(R.string.delete),
                            UiAction.of(UiAction.Type.EDITOR_DELETE), false, 0, 1, 1);
                }
                footerButton(context.getString(R.string.cancel),
                        UiAction.of(UiAction.Type.EDITOR_CANCEL), false, 0, 0, 2);
                footerButton(context.getString(R.string.save),
                        UiAction.of(UiAction.Type.EDITOR_SAVE), true, 1, 0, 2);
                break;
            case DETAILS:
                footerButton(context.getString(R.string.visual_trigger), UiAction.of(UiAction.Type.SHOW_CUE), false, 0, 1, 1);
                footerButton(context.getString(R.string.back), UiAction.of(UiAction.Type.BACK), false, 0, 0, 2);
                footerButton(context.getString(R.string.edit), UiAction.of(UiAction.Type.EDIT_MEMORY), true, 1, 0, 2);
                break;
            case HISTORY:
                footerButton(context.getString(R.string.back), UiAction.of(UiAction.Type.BACK), false, 0, 0, 2);
                footerButton(context.getString(scene.curve ? R.string.bars : R.string.curve), UiAction.of(UiAction.Type.TOGGLE_CHART), true,
                        1, 0, 2);
                break;
            case SETTINGS:
                footerButton(context.getString(R.string.import_action), UiAction.of(UiAction.Type.IMPORT), false, 0, 1, 2);
                footerButton(context.getString(R.string.export_action), UiAction.of(UiAction.Type.EXPORT), true, 1, 1, 2);
                footerButton(context.getString(R.string.back), UiAction.of(UiAction.Type.BACK), false, 0, 0, 1);
                break;
            case CUE:
                footerButton(context.getString(R.string.back), UiAction.of(UiAction.Type.BACK), false, 0, 0, 1);
                break;
            case PROMPT:
                // The native Android input overlay contains Back and Verify.
                break;
        }
    }

    private void footerButton(String label, UiAction action, boolean primary,
            int column, int row, int columns) {
        float width = (right - left - FOOTER_BUTTON_GAP_DP * (columns - 1)) / columns;
        float x = left + column * (width + FOOTER_BUTTON_GAP_DP);
        float y = screenHeight - FOOTER_BOTTOM_MARGIN_DP - (row + 1) * FOOTER_ROW_STEP_DP;
        drawButton(x, y, width, BUTTON_HEIGHT_DP, label, primary);
        footerHits.add(new Hit(new RectF(x, y, x + width, y + BUTTON_HEIGHT_DP), action));
    }

    private void drawButton(float x, float y, float width, float height,
            String label, boolean primary) {
        drawRectangle(x, y, width, height, primary ? PURPLE : PURPLE_SOFT,
                BUTTON_CORNER_RADIUS_DP);
        drawBorder(x, y, width, height);

        int textWidth = Math.max(1, (int) Math.floor(
                width - 2.0f * BUTTON_TEXT_HORIZONTAL_PADDING_DP));
        float textHeight = height - 2.0f * BUTTON_TEXT_VERTICAL_PADDING_DP;
        TextBlock text = fittedButtonText(label, textWidth, textHeight);
        text.draw(canvas, x + BUTTON_TEXT_HORIZONTAL_PADDING_DP,
                y + (height - text.height()) / 2.0f);
    }

    private TextBlock fittedButtonText(String label, int width, float availableHeight) {
        float preferredSize = typography.spToDp(UiTypography.BODY_SP);
        float minimumSize = typography.spToDp(BUTTON_MIN_TEXT_SIZE_SP);
        float sizeStep = typography.spToDp(BUTTON_TEXT_SIZE_STEP_SP);
        String key = width + "|" + availableHeight + "|" + preferredSize + "|" + label;
        TextBlock result = buttonTextBlocks.get(key);
        if (result == null) {
            result = TextBlock.fit(label, width, availableHeight,
                    preferredSize, minimumSize, sizeStep,
                    TEXT, typography.normal(), Layout.Alignment.ALIGN_CENTER);
            buttonTextBlocks.put(key, result);
        }
        return result;
    }

    private void addContentHit(float x, float virtualY, float width, float height, UiAction action) {
        RectF clipped = new RectF(x, virtualY - contentScroll,
                x + width, virtualY - contentScroll + height);
        if (clipped.intersect(viewport)) {
            contentHits.add(new Hit(clipped, action));
        }
    }

    private void drawText(String value, float x, float baseline,
            float size, int color, boolean bold) {
        if (value == null) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(typography.spToDp(size));
        paint.setTypeface(bold ? typography.bold() : typography.normal());
        canvas.drawText(value, x, baseline, paint);
    }

    private TextBlock block(String text, float width, float size, int color, boolean bold) {
        return block(text, width, size, color, bold, Layout.Alignment.ALIGN_NORMAL);
    }

    private TextBlock block(String text, float width, float size, int color, boolean bold,
            Layout.Alignment alignment) {
        int measuredWidth = Math.max(1, (int) Math.floor(width));
        float scaledSize = typography.spToDp(size);
        String cacheKey = measuredWidth + "|" + scaledSize + "|" + color + "|"
                + bold + "|" + alignment + "|" + text;
        TextBlock block = textBlocks.get(cacheKey);
        if (block == null) {
            block = new TextBlock(text, measuredWidth, scaledSize, color,
                    bold ? typography.bold() : typography.normal(), alignment);
            textBlocks.put(cacheKey, block);
        }
        return block;
    }

    private RectF drawStackText(VerticalStack stack, String text,
            float size, int color, boolean bold, float afterGap) {
        TextBlock block = block(text, right - left, size, color, bold);
        RectF bounds = stack.place(block.height(), afterGap);
        block.draw(canvas, bounds.left, bounds.top);
        return bounds;
    }

    private void drawLine(float x1, float y1, float x2, float y2, int color) {
        paint.setColor(color);
        paint.setStrokeWidth(BORDER_STROKE_WIDTH_DP);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawRectangle(float x, float y, float width, float height,
            int color, float radius) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height),
                radius, radius, paint);
    }

    private void drawBorder(float x, float y, float width, float height) {
        paint.setColor(OUTLINE);
        paint.setStrokeWidth(BORDER_STROKE_WIDTH_DP);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(new RectF(x, y, x + width, y + height),
                BUTTON_CORNER_RADIUS_DP, BUTTON_CORNER_RADIUS_DP, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawChart(List<ReviewEvent> events, float x, float y,
            float width, float height, boolean curve, long start, long end) {
        chartRenderer.draw(canvas, events, x, y, width, height,
                curve, start, end, timeFormatter.locale());
    }
}
