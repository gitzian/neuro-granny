package org.gitzian.neurogranny.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;

import java.io.IOException;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.controller.NeuroController;
import org.gitzian.neurogranny.model.CueMode;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;
import org.gitzian.neurogranny.view.CueRenderer;
import org.gitzian.neurogranny.view.UiTypography;

/** Full-page native editor overlay. Cue changes stay in the draft until Save. */
final class MemoryEditor {
    static final int PICK_CUE_IMAGE_REQUEST = 3;
    static final int CAPTURE_CUE_IMAGE_REQUEST = 4;

    private static final int CONTENT_TOP_MARGIN_DP = 66;
    private static final int NEW_EDITOR_BOTTOM_MARGIN_DP = 88;
    private static final int EXISTING_EDITOR_BOTTOM_MARGIN_DP = 140;
    private static final int BACKGROUND_COLOR = 0xff08080b;
    private static final int PREVIEW_BACKGROUND_COLOR = 0xff121116;
    private static final int BUTTON_BACKGROUND_COLOR = 0xff493357;
    private static final int EDITOR_CONTENT_HORIZONTAL_PADDING_DP = 12;
    private static final int EDITOR_CONTENT_TOP_PADDING_DP = 8;
    private static final int EDITOR_CONTENT_BOTTOM_PADDING_DP = 18;
    private static final int EDITOR_MAX_CONTENT_WIDTH_DP = 520;
    private static final int EDITOR_HORIZONTAL_MARGIN_DP = 32;
    private static final int EDITOR_MIN_CONTENT_WIDTH_DP = 200;
    private static final int PROTECTION_ROW_HEIGHT_DP = 52;
    private static final int CUE_PREVIEW_SIZE_DP = 220;
    private static final int CUE_CONTROL_TOP_MARGIN_DP = 10;
    private static final int CUE_ACTION_BUTTON_HEIGHT_DP = 48;
    private static final int EMPTY_PREVIEW_PADDING_DP = 8;
    private static final int CHOOSE_EXISTING_PHOTO_INDEX = 0;

    private final Activity activity;
    private final NeuroController controller;
    private final FrameLayout root;
    private final FormInputs inputs;
    private final CueImageProcessor imageProcessor;
    private final AppDialogs dialogs;
    private EditorForm formState;

    MemoryEditor(Activity activity, NeuroController controller, FrameLayout root,
            FormInputs inputs, AppDialogs dialogs) {
        this.activity = activity;
        this.controller = controller;
        this.root = root;
        this.inputs = inputs;
        this.imageProcessor = new CueImageProcessor(activity);
        this.dialogs = dialogs;
    }

    /** Native fields hosted inside the standard OpenGL page header/footer. */
    private static final class EditorForm {
        Memory original;
        ScrollView scroll;
        EditText name;
        EditText secret;
        EditText repeatSecret;
        Spinner protection;
        Spinner schedule;
        Spinner cueMode;
        LinearLayout fixed;
        LinearLayout min;
        LinearLayout max;
        FrameLayout preview;
        Button cueAction;
        TextView photoHint;
        byte[] cueImage;
        long patternSeed;
        long stablePatternSeed;
        boolean changingSecret;
        boolean regenerated;

        CueMode selectedCueMode() {
            return CueMode.fromDatabaseValue(cueMode.getSelectedItemPosition());
        }
    }

    private View cuePreview(Memory memory, byte[] photo, long seed, CueMode mode) {
        if (mode == CueMode.PHOTO) {
            if (photo != null && photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                if (bitmap != null) {
                    ImageView image = new ImageView(activity);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    image.setImageBitmap(bitmap);
                    return image;
                }
            }
            TextView empty = inputs.label(activity.getString(R.string.choose_custom_photo));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(EMPTY_PREVIEW_PADDING_DP), dp(EMPTY_PREVIEW_PADDING_DP),
                    dp(EMPTY_PREVIEW_PADDING_DP), dp(EMPTY_PREVIEW_PADDING_DP));
            return empty;
        }

        Memory previewMemory = memory == null ? new Memory() : memory.copy();
        previewMemory.patternSeed = seed;
        previewMemory.plain = "";
        previewMemory.verifier = "";
        previewMemory.cueMode = CueMode.GENERATED;
        previewMemory.cueImage = null;
        return new View(activity) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float size = Math.min(getWidth(), getHeight());
                CueRenderer.render(canvas, previewMemory,
                        (getWidth() - size) / 2.0f,
                        (getHeight() - size) / 2.0f, size);
            }
        };
    }

    private void refreshCueEditorState() {
        EditorForm form = formState;
        if (form == null) {
            return;
        }
        CueMode mode = form.selectedCueMode();
        form.cueAction.setText(mode == CueMode.PHOTO
                ? R.string.select_photo : R.string.regenerate_visual_trigger);
        form.photoHint.setVisibility(mode == CueMode.PHOTO ? View.VISIBLE : View.GONE);
        form.preview.removeAllViews();
        form.preview.addView(cuePreview(form.original, form.cueImage, form.patternSeed, mode),
                new FrameLayout.LayoutParams(-1, -1));
    }

    void dismiss() {
        EditorForm form = formState;
        formState = null;
        if (form == null) {
            return;
        }
        ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(form.scroll.getWindowToken(), 0);
        form.scroll.clearFocus();
        if (form.scroll.getParent() instanceof ViewGroup) {
            ((ViewGroup) form.scroll.getParent()).removeView(form.scroll);
        }
    }

    void show(Memory memory) {
        dismiss();
        final boolean fresh = memory == null;
        Memory initial = fresh ? new Memory() : memory.copy();
        EditorForm form = new EditorForm();
        form.original = fresh ? null : initial.copy();
        if (form.original != null) {
            form.original.plain = "";
            form.original.verifier = "";
        }
        form.cueImage = initial.cueImage == null ? null : initial.cueImage.clone();
        form.patternSeed = fresh ? controller.previewPatternSeed(0L) : initial.patternSeed;
        form.stablePatternSeed = form.patternSeed;

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(BACKGROUND_COLOR);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        SensitiveInputPrivacy.excludeForm(scroll);
        form.scroll = scroll;

        FrameLayout wrapper = new FrameLayout(activity);
        scroll.addView(wrapper, new ScrollView.LayoutParams(-1, -2));
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(EDITOR_CONTENT_HORIZONTAL_PADDING_DP),
                dp(EDITOR_CONTENT_TOP_PADDING_DP),
                dp(EDITOR_CONTENT_HORIZONTAL_PADDING_DP),
                dp(EDITOR_CONTENT_BOTTOM_PADDING_DP));
        SensitiveInputPrivacy.excludeForm(content);
        FrameLayout.LayoutParams contentLayout = new FrameLayout.LayoutParams(
                Math.min(dp(EDITOR_MAX_CONTENT_WIDTH_DP),
                        activity.getResources().getDisplayMetrics().widthPixels
                                - dp(EDITOR_HORIZONTAL_MARGIN_DP)),
                -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        wrapper.addView(content, contentLayout);
        scroll.addOnLayoutChangeListener((v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            int available = Math.max(dp(EDITOR_MIN_CONTENT_WIDTH_DP),
                    Math.min(dp(EDITOR_MAX_CONTENT_WIDTH_DP),
                            right - left - dp(EDITOR_HORIZONTAL_MARGIN_DP)));
            if (content.getLayoutParams().width != available) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
                params.width = available;
                content.setLayoutParams(params);
            }
        });

        TextView heading = inputs.label(activity.getString(fresh ? R.string.add_memory : R.string.edit_memory));
        heading.setTextSize(UiTypography.TITLE_SP);
        heading.setTextColor(Color.WHITE);
        heading.setTypeface(heading.getTypeface(), Typeface.BOLD);
        content.addView(heading);

        content.addView(inputs.label(activity.getString(R.string.name)));
        form.name = inputs.entry(fresh ? "" : initial.title, activity.getString(R.string.hint_name_example), false);
        content.addView(form.name);
        content.addView(inputs.label(activity.getString(R.string.password_protection)));
        LinearLayout protectionRow = new LinearLayout(activity);
        protectionRow.setOrientation(LinearLayout.HORIZONTAL);
        form.protection = inputs.spinner(new String[] {
                activity.getString(R.string.recoverable),
                activity.getString(R.string.verify_only)
        }, initial.secretMode().databaseValue());
        protectionRow.addView(form.protection, new LinearLayout.LayoutParams(0, dp(PROTECTION_ROW_HEIGHT_DP), 1));
        Button help = button("?");
        protectionRow.addView(help, new LinearLayout.LayoutParams(dp(PROTECTION_ROW_HEIGHT_DP), dp(PROTECTION_ROW_HEIGHT_DP)));
        help.setOnClickListener(v -> dialogs.alert(activity.getString(R.string.password_protection),
                activity.getString(R.string.password_protection_explanation)));
        content.addView(protectionRow);

        content.addView(inputs.label(activity.getString(fresh ? R.string.password : R.string.new_password_blank)));
        form.secret = inputs.entry("", activity.getString(fresh ? R.string.hint_enter_password
                : R.string.hint_password_unchanged), true);
        content.addView(form.secret);
        TextView repeatLabel = inputs.label(activity.getString(R.string.repeat_password));
        form.repeatSecret = inputs.entry("", activity.getString(R.string.hint_repeat_password), true);
        content.addView(repeatLabel);
        content.addView(form.repeatSecret);
        repeatLabel.setVisibility(fresh ? View.VISIBLE : View.GONE);
        form.repeatSecret.setVisibility(fresh ? View.VISIBLE : View.GONE);

        if (!fresh) {
            form.secret.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence text, int start, int before, int count) {
                    boolean changed = text.length() > 0;
                    repeatLabel.setVisibility(changed ? View.VISIBLE : View.GONE);
                    form.repeatSecret.setVisibility(changed ? View.VISIBLE : View.GONE);
                    if (changed != form.changingSecret) {
                        form.changingSecret = changed;
                        form.patternSeed = changed
                                ? controller.previewPatternSeed(form.stablePatternSeed)
                                : form.stablePatternSeed;
                        refreshCueEditorState();
                    }
                }

                @Override
                public void afterTextChanged(Editable text) {
                }
            });
        }

        content.addView(inputs.label(activity.getString(R.string.visual_trigger)));
        form.cueMode = inputs.spinner(new String[] {
                activity.getString(R.string.generated_pattern),
                activity.getString(R.string.custom_photo)
        }, initial.cueMode.databaseValue());
        content.addView(form.cueMode);

        form.preview = new FrameLayout(activity);
        LinearLayout.LayoutParams previewLayout = new LinearLayout.LayoutParams(dp(CUE_PREVIEW_SIZE_DP), dp(CUE_PREVIEW_SIZE_DP));
        previewLayout.gravity = Gravity.CENTER_HORIZONTAL;
        previewLayout.topMargin = dp(CUE_CONTROL_TOP_MARGIN_DP);
        form.preview.setBackgroundColor(PREVIEW_BACKGROUND_COLOR);
        content.addView(form.preview, previewLayout);

        form.cueAction = button(activity.getString(R.string.regenerate_visual_trigger));
        LinearLayout.LayoutParams actionLayout = new LinearLayout.LayoutParams(-1, dp(CUE_ACTION_BUTTON_HEIGHT_DP));
        actionLayout.topMargin = dp(CUE_CONTROL_TOP_MARGIN_DP);
        content.addView(form.cueAction, actionLayout);
        form.photoHint = inputs.label(activity.getString(R.string.custom_photo_hint));
        content.addView(form.photoHint);

        form.cueMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshCueEditorState();
            }
        });
        form.cueAction.setOnClickListener(v -> {
            if (form.selectedCueMode() == CueMode.PHOTO) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.select_photo)
                        .setItems(new String[] {
                                activity.getString(R.string.choose_photo),
                                activity.getString(R.string.take_photo)
                        }, (dialog, choice) -> {
                            Intent intent;
                            int request;
                            if (choice == CHOOSE_EXISTING_PHOTO_INDEX) {
                                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("image/*");
                                request = PICK_CUE_IMAGE_REQUEST;
                            } else {
                                intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                request = CAPTURE_CUE_IMAGE_REQUEST;
                            }
                            activity.startActivityForResult(intent, request);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                dialogs.confirm(activity.getString(R.string.regenerate_visual_trigger),
                        activity.getString(R.string.regenerate_visual_trigger_confirmation), () -> {
                            if (formState != form) {
                                return;
                            }
                            form.patternSeed = controller.previewPatternSeed(form.patternSeed);
                            form.stablePatternSeed = form.patternSeed;
                            form.regenerated = true;
                            refreshCueEditorState();
                        });
            }
        });

        content.addView(inputs.label(activity.getString(R.string.review_schedule)));
        form.schedule = inputs.spinner(new String[] {
                activity.getString(R.string.schedule_fixed),
                activity.getString(R.string.schedule_auto),
                activity.getString(R.string.schedule_ask_next)
        }, initial.schedule().databaseValue());
        content.addView(form.schedule);
        form.fixed = inputs.intervalControl(initial.fixedSeconds, activity.getString(R.string.fixed_initial_interval));
        content.addView(form.fixed);
        form.min = inputs.intervalControl(initial.minSeconds, activity.getString(R.string.auto_min_interval));
        content.addView(form.min);
        form.max = inputs.intervalControl(initial.maxSeconds, activity.getString(R.string.auto_max_interval));
        content.addView(form.max);
        form.schedule.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean adaptive = position == ScheduleMode.AUTO_LEARNER.databaseValue();
                form.fixed.setVisibility(adaptive ? View.GONE : View.VISIBLE);
                form.min.setVisibility(adaptive ? View.VISIBLE : View.GONE);
                form.max.setVisibility(adaptive ? View.VISIBLE : View.GONE);
            }
        });

        formState = form;
        refreshCueEditorState();
        FrameLayout.LayoutParams editorLayout = new FrameLayout.LayoutParams(-1, -1);
        editorLayout.topMargin = dp(CONTENT_TOP_MARGIN_DP);
        // Match UiPainter's viewport so the GL footer remains clickable.
        editorLayout.bottomMargin = dp(fresh ? NEW_EDITOR_BOTTOM_MARGIN_DP
                : EXISTING_EDITOR_BOTTOM_MARGIN_DP);
        root.addView(scroll, editorLayout);
    }

    void save() {
        EditorForm form = formState;
        if (form == null) {
            return;
        }
        NeuroController.EditorDraft draft = new NeuroController.EditorDraft();
        draft.title = form.name.getText().toString();
        draft.newSecret = form.secret.getText().toString();
        draft.repeatedSecret = form.repeatSecret.getText().toString();
        draft.regeneratePattern = form.regenerated;
        draft.cueEdited = true;
        draft.hasPatternSeed = true;
        draft.patternSeed = form.patternSeed;
        draft.mode = SecretMode.fromDatabaseValue(form.protection.getSelectedItemPosition());
        draft.scheduleMode = ScheduleMode.fromDatabaseValue(form.schedule.getSelectedItemPosition());
        draft.cueMode = form.selectedCueMode();
        draft.cueImage = form.cueImage == null ? null : form.cueImage.clone();
        try {
            draft.fixedSeconds = inputs.readInterval(form.fixed);
            draft.minSeconds = inputs.readInterval(form.min);
            draft.maxSeconds = inputs.readInterval(form.max);
            if (draft.title.trim().isEmpty() || (form.original == null && draft.newSecret.isEmpty())
                    || draft.maxSeconds < draft.minSeconds) {
                throw new IllegalArgumentException(activity.getString(R.string.check_entry_fields));
            }
            if (!draft.newSecret.isEmpty() && !draft.newSecret.equals(draft.repeatedSecret)) {
                dialogs.alert(activity.getString(R.string.passwords_differ), activity.getString(R.string.passwords_must_match));
                return;
            }
            // The editor remains visible through confirmations and failed verification.
            controller.saveEditor(draft, saved -> {
                // On success the controller navigates away; render() removes the form.
            });
        } catch (Exception ex) {
            dialogs.alert(activity.getString(R.string.invalid_entry), activity.getString(R.string.check_entry_fields));
        }
    }

    boolean handleActivityResult(int request, int result, Intent data) {
        if (request != PICK_CUE_IMAGE_REQUEST && request != CAPTURE_CUE_IMAGE_REQUEST) {
            return false;
        }
        if (result != Activity.RESULT_OK || formState == null) {
            return true;
        }
        try {
            byte[] image;
            if (request == CAPTURE_CUE_IMAGE_REQUEST) {
                Object raw = data == null || data.getExtras() == null
                        ? null : data.getExtras().get("data");
                if (!(raw instanceof Bitmap)) {
                    throw new IOException(activity.getString(R.string.invalid_image));
                }
                image = imageProcessor.encodeCueBitmap((Bitmap) raw);
            } else {
                if (data == null || data.getData() == null) {
                    return true;
                }
                image = imageProcessor.readCueImage(data.getData());
            }
            formState.cueImage = image;
            refreshCueEditorState();
        } catch (Exception exception) {
            dialogs.alert(activity.getString(R.string.file_operation_failed),
                    activity.getString(R.string.invalid_image));
        }
        return true;
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private Button button(String label) {
        Button button = new Button(activity);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(UiTypography.BODY_SP);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BUTTON_BACKGROUND_COLOR));
        return button;
    }

}
