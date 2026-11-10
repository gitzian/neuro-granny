package org.gitzian.neurogranny.android;

import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

/**
 * Applies Android's privacy hints to native input controls.
 *
 * <p>These flags opt the app out of normal Autofill and Content Capture flows and
 * request that the keyboard not learn entered text. They are not a security
 * boundary against explicit user actions, privileged services, or untrusted IMEs.
 */
public final class SensitiveInputPrivacy {

    private SensitiveInputPrivacy() {
    }

    /**
     * Prevents normal Autofill and Content Capture traversal into an input form.
     *
     * <p>The older Android versions supported by Neuro Granny do not expose all
     * of these platform APIs; the availability checks keep the form functional.
     */
    public static void excludeForm(View form) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            form.setImportantForAutofill(
                    View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            form.setImportantForContentCapture(
                    View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS);
        }
    }

    /**
     * Protects an individual field, including a field outside the standard form.
     * Applies the keyboard learning hint to titles as well as passwords.
     */
    public static void protectTextField(EditText field) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            field.setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO);
        }

        setImeOptions(field, field.getImeOptions());
    }

    /**
     * Requests masked password input and applies the same privacy hints.
     */
    public static void configurePasswordField(EditText field) {
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        protectTextField(field);
    }

    /**
     * Changes only the IME action while keeping other IME flags intact.
     */
    public static void setImeAction(EditText field, int action) {
        int existingOptions = field.getImeOptions();
        int retainedFlags = existingOptions & ~EditorInfo.IME_MASK_ACTION;
        setImeOptions(field, retainedFlags | action);
    }

    private static void setImeOptions(EditText field, int options) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            options |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        }

        field.setImeOptions(options);
    }
}
