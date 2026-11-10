package org.gitzian.neurogranny.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/** Local preference and API-23-compatible configuration for the app's text resources. */
public final class AppLanguage {
    private static final String PREFERENCES = "neurogranny_preferences";
    private static final String LANGUAGE_KEY = "language";
    public static final String SYSTEM = "system";

    public enum Choice {
        SYSTEM_DEFAULT("system"),
        ENGLISH("en"),
        GERMAN("de"),
        FRENCH("fr"),
        SPANISH("es"),
        ITALIAN("it"),
        POLISH("pl"),
        PORTUGUESE_BRAZIL("pt-BR"),
        DUTCH("nl");

        public final String tag;

        Choice(String tag) {
            this.tag = tag;
        }

        /** Native language names come from Java Locale, not hardcoded UI labels. */
        public String nativeName() {
            if (this == SYSTEM_DEFAULT) {
                throw new IllegalStateException("Use the localized system-default resource");
            }
            Locale locale = Locale.forLanguageTag(tag);
            String name = locale.getDisplayName(locale);
            if (name.isEmpty()) {
                return tag;
            }
            return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
        }
    }

    private AppLanguage() {
    }

    public static Choice selection(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String storedTag = preferences.getString(LANGUAGE_KEY, SYSTEM);
        for (Choice choice : Choice.values()) {
            if (choice.tag.equals(storedTag)) {
                return choice;
            }
        }
        return Choice.SYSTEM_DEFAULT;
    }

    public static void select(Context context, Choice choice) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(LANGUAGE_KEY, choice.tag)
                .apply();
    }

    public static Context localizedContext(Context context) {
        Locale selected = resolve(context);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(selected);
        return context.createConfigurationContext(configuration);
    }

    private static Locale resolve(Context context) {
        Choice choice = selection(context);
        if (choice != Choice.SYSTEM_DEFAULT) {
            return Locale.forLanguageTag(choice.tag);
        }

        Configuration configuration = context.getResources().getConfiguration();
        Locale systemLocale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? configuration.getLocales().get(0) : configuration.locale;
        if (systemLocale == null) {
            return Locale.ENGLISH;
        }
        String language = systemLocale.getLanguage();
        if ("pt".equals(language)) {
            return "BR".equalsIgnoreCase(systemLocale.getCountry())
                    ? Locale.forLanguageTag("pt-BR") : Locale.ENGLISH;
        }
        for (Choice supported : Choice.values()) {
            if (supported != Choice.SYSTEM_DEFAULT && supported.tag.equals(language)) {
                return systemLocale;
            }
        }
        return Locale.ENGLISH;
    }
}
