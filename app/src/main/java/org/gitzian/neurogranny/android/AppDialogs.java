package org.gitzian.neurogranny.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.Calendar;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.model.Callback;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.TimeUnit;
import org.gitzian.neurogranny.view.CueRenderer;

/** Native confirmation, scheduling, and password-entry dialogs. */
final class AppDialogs {
    private static final int PASSWORD_CUE_SIZE_DP = 64;
    private static final int PASSWORD_FIELD_HEIGHT_DP = 60;
    private static final int PASSWORD_DIALOG_HORIZONTAL_PADDING_DP = 20;
    private static final int PASSWORD_DIALOG_TOP_PADDING_DP = 4;
    private static final int PASSWORD_DIALOG_BOTTOM_PADDING_DP = 8;
    private static final int PASSWORD_FIELD_START_MARGIN_DP = 8;
    private final Activity activity;
    private final FormInputs inputs;

    AppDialogs(Activity activity, FormInputs inputs) {
        this.activity = activity;
        this.inputs = inputs;
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    void chooseLanguage() {
        AppLanguage.Choice[] choices = AppLanguage.Choice.values();
        String[] labels = new String[choices.length];
        int selectedIndex = AppLanguage.selection(activity).ordinal();
        for (int index = 0; index < choices.length; index++) {
            labels[index] = choices[index] == AppLanguage.Choice.SYSTEM_DEFAULT
                    ? activity.getString(R.string.system_default) : choices[index].nativeName();
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.language)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, selected) -> {
                    dialog.dismiss();
                    AppLanguage.Choice choice = choices[selected];
                    if (choice != AppLanguage.selection(activity)) {
                        AppLanguage.select(activity, choice);
                        activity.recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    void alert(String title, String body) {
        new AlertDialog.Builder(activity).setTitle(title).setMessage(body).setPositiveButton(R.string.ok, null).show();
    }

    void confirm(String title, String body, Runnable yes) {
        new AlertDialog.Builder(activity).setTitle(title).setMessage(body).setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes,(d, w) -> yes.run()).show();
    }

    void confidence(Callback<Boolean> next) {
        // The review must settle before leaving the prompt.
        new AlertDialog.Builder(activity)
                .setTitle(R.string.did_it_come_easily)
                .setNegativeButton(R.string.no, (dialog, which) -> next.accept(false))
                .setPositiveButton(R.string.yes, (dialog, which) -> next.accept(true))
                .setCancelable(false)
                .show();
    }

    void nextInterval(Callback<Long> next) {
        LinearLayout row = inputs.intervalControl(TimeUnit.DAYS.seconds(), activity.getString(R.string.next_review_interval));
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.when_next).setView(row)
                .setCancelable(false).setPositiveButton(R.string.schedule_button, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long seconds = inputs.readInterval(row);
                dialog.dismiss();
                next.accept(seconds);
            } catch (Exception ex) {
                alert(activity.getString(R.string.invalid_interval), activity.getString(R.string.invalid_interval_value));
            }
        }));
        dialog.show();
    }

    void chooseDate(long millis, Callback<Long> next) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        new DatePickerDialog(activity,(date, y, mo, day) -> {
            Calendar v = Calendar.getInstance();
            v.set(y, mo, day);
            new TimePickerDialog(activity,(time, h, m) -> {
                v.set(Calendar.HOUR_OF_DAY, h);
                v.set(Calendar.MINUTE, m);
                v.set(Calendar.SECOND, 0);
                v.set(Calendar.MILLISECOND, 0);
                next.accept(v.getTimeInMillis());
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    void requestEditPassword(Memory selectedMemory, Callback<String> callback) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        SensitiveInputPrivacy.excludeForm(container);
        container.setPadding(dp(PASSWORD_DIALOG_HORIZONTAL_PADDING_DP),
                dp(PASSWORD_DIALOG_TOP_PADDING_DP), dp(PASSWORD_DIALOG_HORIZONTAL_PADDING_DP),
                dp(PASSWORD_DIALOG_BOTTOM_PADDING_DP));

        // Every password challenge keeps its established visual recall cue.
        if (selectedMemory != null) {
            View cue = new View(activity) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    float size = Math.min(getWidth(), getHeight());
                    CueRenderer.render(canvas, selectedMemory,
                            (getWidth() - size) / 2.0f, (getHeight() - size) / 2.0f, size);
                }
            };
            container.addView(cue, new LinearLayout.LayoutParams(dp(PASSWORD_CUE_SIZE_DP), dp(PASSWORD_CUE_SIZE_DP)));
        }

        EditText password = inputs.entry("", activity.getString(R.string.current_password), true);
        SensitiveInputPrivacy.setImeAction(password, EditorInfo.IME_ACTION_DONE);
        LinearLayout.LayoutParams passwordLayout =
                new LinearLayout.LayoutParams(0, dp(PASSWORD_FIELD_HEIGHT_DP), 1.0f);
        passwordLayout.setMarginStart(dp(PASSWORD_FIELD_START_MARGIN_DP));
        container.addView(password, passwordLayout);

        AlertDialog verification = new AlertDialog.Builder(activity)
                .setTitle(R.string.confirm_current_password)
                .setView(container)
                .setNegativeButton(R.string.cancel, (dialog, which) -> callback.accept(null))
                .setPositiveButton(R.string.verify, (dialog, which) ->
                        callback.accept(password.getText().toString()))
                .create();
        verification.setOnShowListener(ignored -> {
            if (verification.getWindow() != null) {
                verification.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            password.requestFocus();
        });
        verification.show();
    }

}
