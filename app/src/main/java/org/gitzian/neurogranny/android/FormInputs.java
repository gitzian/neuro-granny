package org.gitzian.neurogranny.android;

import android.app.Activity;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.TimeUnit;
import org.gitzian.neurogranny.view.UiTypography;

/** Shared native input controls and system-locale interval parsing. */
final class FormInputs {
    private static final int FIELD_HEIGHT_DP = 48;
    private static final int LABEL_TOP_PADDING_DP = 12;
    private static final int LABEL_TEXT_COLOR = 0xffc0bac5;
    private static final int INPUT_HINT_COLOR = 0xff9e9aa2;

    private final Activity activity;

    FormInputs(Activity activity) {
        this.activity = activity;
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    TextView label(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(LABEL_TEXT_COLOR);
        t.setTextSize(UiTypography.BODY_SP);
        t.setPadding(0, dp(LABEL_TOP_PADDING_DP), 0, 0);
        return t;
    }

    EditText entry(String value, String hint, boolean password) {
        EditText e = new EditText(activity);
        e.setSingleLine(true);
        e.setText(value);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(INPUT_HINT_COLOR);
        e.setTextSize(UiTypography.BODY_SP);
        if (password) {
            SensitiveInputPrivacy.configurePasswordField(e);
        } else {
            SensitiveInputPrivacy.protectTextField(e);
        }
        e.setTypeface(UiTypography.defaultTypeface(activity));
        return e;
    }

    Spinner spinner(String[] options, int index) {
        Spinner sp = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_dropdown_item, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return bodyText(super.getView(position, convertView, parent));
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return bodyText(super.getDropDownView(position, convertView, parent));
            }

            private View bodyText(View view) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextSize(UiTypography.BODY_SP);
                }
                return view;
            }
        };
        sp.setAdapter(adapter);
        sp.setSelection(index);
        return sp;
    }

    /** Interval row uses exactly the seconds/minutes/hours/days selector. */
    LinearLayout intervalControl(long seconds, String title) {
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(label(title));
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TimeUnit unit = TimeUnit.bestFor(seconds);
        EditText amount = entry(Interval.amount(seconds, unit), "1", false);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        SensitiveInputPrivacy.protectTextField(amount);
        row.addView(amount, new LinearLayout.LayoutParams(0, dp(FIELD_HEIGHT_DP), 1));
        String[] units = {
                activity.getString(R.string.unit_seconds),
                activity.getString(R.string.unit_minutes),
                activity.getString(R.string.unit_hours),
                activity.getString(R.string.unit_days)
        };
        Spinner sp = spinner(units, unit.ordinal());
        row.addView(sp, new LinearLayout.LayoutParams(0, dp(FIELD_HEIGHT_DP), 1));
        column.addView(row);
        column.setTag(new Object[] {
            amount, sp
        });
        return column;
    }

    long readInterval(LinearLayout column) {
        Object[] parts = (Object[]) column.getTag();
        TimeUnit unit = TimeUnit.atIndex(((Spinner) parts[1]).getSelectedItemPosition());
        String input = ((EditText) parts[0]).getText().toString().trim();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setGroupingUsed(false);
        ParsePosition position = new ParsePosition(0);
        Number number = numberFormat.parse(input, position);
        if (number == null || position.getIndex() != input.length()) {
            throw new IllegalArgumentException(activity.getString(R.string.invalid_interval_value));
        }
        try {
            return Interval.from(number.doubleValue(), unit);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(activity.getString(R.string.invalid_interval_value), exception);
        }
    }

}
