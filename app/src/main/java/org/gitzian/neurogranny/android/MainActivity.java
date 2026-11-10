package org.gitzian.neurogranny.android;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.List;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.backup.BackupService;
import org.gitzian.neurogranny.controller.NeuroController;
import org.gitzian.neurogranny.data.MemoryRepository;
import org.gitzian.neurogranny.data.NeuroDatabase;
import org.gitzian.neurogranny.model.Callback;
import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.schedule.ReminderScheduler;
import org.gitzian.neurogranny.view.GlUiView;
import org.gitzian.neurogranny.view.UiAction;
import org.gitzian.neurogranny.view.UiScene;
import org.gitzian.neurogranny.view.UiTypography;

/** One Activity hosts OpenGL ES 2.0 and a native EditText input overlay. */
public final class MainActivity extends Activity implements GlUiView.Listener, NeuroController.Host {
    private static final String SUPPORT_URL = "https://buy.stripe.com/cNibJ21uU4fhgM65P9cjS00";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 51;
    private static final int SCREEN_BACKGROUND_COLOR = 0xff08080b;
    private static final int ANSWER_HINT_COLOR = 0xffaaa3b0;
    private static final int BUTTON_BACKGROUND_COLOR = 0xff493357;
    private static final int INPUT_HORIZONTAL_PADDING_DP = 12;
    private static final int INPUT_VERTICAL_PADDING_DP = 6;
    private static final int ANSWER_HEIGHT_DP = 54;
    private static final int FOOTER_BUTTON_HEIGHT_DP = 48;
    private static final int FOOTER_EDGE_MARGIN_DP = 2;
    private static final int FOOTER_BUTTON_GAP_DP = 4;
    private static final int KEYBOARD_SHOW_DELAY_MS = 120;
    private static final long NO_NOTIFICATION_REVIEW_ID = 0L;
    private NeuroDatabase db;
    private MemoryRepository repo;
    private ReminderScheduler scheduler;
    private NeuroController controller;
    private GlUiView gl;
    private LinearLayout inputOverlay;
    private EditText answer;
    private Button verify;
    private UiScene.Page currentPage = UiScene.Page.HOME;
    private long notificationReviewId = NO_NOTIFICATION_REVIEW_ID;
    private AppDialogs dialogs;
    private MemoryEditor memoryEditor;
    private BackupCoordinator backupCoordinator;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AppLanguage.localizedContext(base));
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Own the insets so the keyboard is not accounted for twice.
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().setStatusBarColor(Color.rgb(8, 8, 11));
        getWindow().setNavigationBarColor(Color.rgb(8, 8, 11));
        db = new NeuroDatabase(this);
        repo = new MemoryRepository(db);
        scheduler = new ReminderScheduler(this, repo);
        BackupService backups = new BackupService(this, repo);
        buildShell();
        controller = new NeuroController(repo, scheduler, this);
        FormInputs inputs = new FormInputs(this);
        dialogs = new AppDialogs(this, inputs);
        memoryEditor = new MemoryEditor(this, controller, (FrameLayout) gl.getParent(), inputs, dialogs);
        backupCoordinator = new BackupCoordinator(this, repo, backups, scheduler, controller, dialogs);
        controller.refresh();
        scheduler.refresh();
        routeIntent(getIntent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SCREEN_BACKGROUND_COLOR);
        gl = new GlUiView(this);
        gl.setListener(this);
        root.addView(gl, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets systemBars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard = insets.getInsets(WindowInsets.Type.ime());

                root.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        Math.max(systemBars.bottom, keyboard.bottom));
                return insets;
            });
        } else {
            root.setFitsSystemWindows(true);
        }
        inputOverlay = new LinearLayout(this);
        inputOverlay.setOrientation(LinearLayout.VERTICAL);
        SensitiveInputPrivacy.excludeForm(inputOverlay);
        inputOverlay.setBackgroundColor(SCREEN_BACKGROUND_COLOR);
        inputOverlay.setPadding(dp(INPUT_HORIZONTAL_PADDING_DP), dp(INPUT_VERTICAL_PADDING_DP),
                dp(INPUT_HORIZONTAL_PADDING_DP), dp(INPUT_HORIZONTAL_PADDING_DP));
        answer = new EditText(this);
        answer.setSingleLine(true);
        SensitiveInputPrivacy.configurePasswordField(answer);
        answer.setTextColor(Color.WHITE);
        answer.setHintTextColor(ANSWER_HINT_COLOR);
        answer.setHint(R.string.hint_enter_password);
        answer.setTextSize(UiTypography.BODY_SP);
        answer.setTypeface(UiTypography.defaultTypeface(this));
        SensitiveInputPrivacy.setImeAction(answer, EditorInfo.IME_ACTION_DONE);
        inputOverlay.addView(answer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ANSWER_HEIGHT_DP)));
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button back = button(getString(R.string.back));
        verify = button(getString(R.string.verify));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(FOOTER_BUTTON_HEIGHT_DP), 1);
        p.setMargins(dp(FOOTER_EDGE_MARGIN_DP), 0, dp(FOOTER_BUTTON_GAP_DP), 0);
        controls.addView(back, p);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(FOOTER_BUTTON_HEIGHT_DP), 1);
        p2.setMargins(dp(FOOTER_BUTTON_GAP_DP), 0, dp(FOOTER_EDGE_MARGIN_DP), 0);
        controls.addView(verify, p2);
        back.setOnClickListener(v -> controller.back());
        verify.setOnClickListener(v -> submitAnswer());
        answer.setOnEditorActionListener((v, id, event) -> {
            submitAnswer();
            return true;
        });
        inputOverlay.addView(controls);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(inputOverlay, bottom);
        inputOverlay.setVisibility(View.GONE);
        setContentView(root);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(UiTypography.BODY_SP);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BUTTON_BACKGROUND_COLOR));
        return b;
    }

    private void submitAnswer() {
        controller.submitAnswer(answer.getText().toString());
    }

    @Override
    public void render(UiScene scene) {
        currentPage = scene.page;
        if (scene.page != UiScene.Page.PROMPT) {
            notificationReviewId = NO_NOTIFICATION_REVIEW_ID;
        }
        if (scene.page != UiScene.Page.EDITOR) {
            memoryEditor.dismiss();
        }
        gl.show(scene);
        boolean prompt = scene.page == UiScene.Page.PROMPT;
        if ((inputOverlay.getVisibility() == View.VISIBLE) != prompt) {
            inputOverlay.setVisibility(prompt ? View.VISIBLE : View.GONE);
            if (prompt) {
                answer.requestFocus();
                answer.postDelayed(() -> {
                    if (currentPage == UiScene.Page.PROMPT) ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .showSoftInput(answer, InputMethodManager.SHOW_IMPLICIT);
                }, KEYBOARD_SHOW_DELAY_MS);
            }
            if (!prompt) {
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(answer.getWindowToken(), 0);
                answer.clearFocus();
            }
        }
    }

    @Override
    public void busy(boolean value) {
        verify.setEnabled(!value);
    }

    @Override
    public void clearAnswer() {
        answer.setText("");
    }

    @Override
    public void onUiThread(Runnable action) {
        runOnUiThread(action);
    }

    @Override
    public void onAction(UiAction action) {
        controller.act(action);
    }

    @Override
    public void onScroll(int deltaDp, int maxScroll) {
        controller.scroll(deltaDp, maxScroll);
    }

    @Override
    public void onBackPressed() {
        controller.back();
    }

    @Override
    public void exitToSystem() {
        // Delegate root Back to Android, including its task/background behavior.
        super.onBackPressed();
    }

    @Override
    public boolean finishNotificationReview(long memoryId) {
        if (notificationReviewId != memoryId || currentPage != UiScene.Page.PROMPT) {
            return false;
        }
        notificationReviewId = NO_NOTIFICATION_REVIEW_ID;
        finish();
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        routeIntent(intent);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().requestApplyInsets();
        }
        if (controller != null) {
            controller.refresh();
        }
    }

    private void routeIntent(Intent intent) {
        if (intent == null || controller == null) {
            return;
        }
        notificationReviewId = NO_NOTIFICATION_REVIEW_ID;
        long id = intent.getLongExtra(ReminderScheduler.EXTRA_MEMORY_ID, NO_NOTIFICATION_REVIEW_ID);
        if (id != NO_NOTIFICATION_REVIEW_ID) {
            intent.removeExtra(ReminderScheduler.EXTRA_MEMORY_ID);
            if (controller.openFromNotification(id)) {
                notificationReviewId = id;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gl != null) {
            gl.onResume();
        }
        if (controller != null) {
            scheduler.refresh();
            controller.refresh();
        }
    }

    @Override
    protected void onPause() {
        if (gl != null) {
            gl.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.shutdown();
        }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    public String text(int resourceId, Object... arguments) {
        return getString(resourceId, arguments);
    }

    @Override
    public void chooseLanguage() {
        dialogs.chooseLanguage();
    }

    @Override
    public void alert(String title, String body) {
        dialogs.alert(title, body);
    }

    @Override
    public void confirm(String title, String body, Runnable yes) {
        dialogs.confirm(title, body, yes);
    }

    @Override
    public void confidence(Callback<Boolean> next) {
        dialogs.confidence(next);
    }

    @Override
    public void nextInterval(Callback<Long> next) {
        dialogs.nextInterval(next);
    }

    @Override
    public void chooseDate(long millis, Callback<Long> next) {
        dialogs.chooseDate(millis, next);
    }

    @Override
    public void editor(Memory memory) {
        memoryEditor.show(memory);
    }

    @Override
    public void saveEditorForm() {
        memoryEditor.save();
    }

    @Override
    public void requestEditPassword(Callback<String> callback) {
        dialogs.requestEditPassword(repo.get(controller.selectedMemoryId()), callback);
    }

    @Override
    public void exportDatabase() {
        backupCoordinator.exportDatabase();
    }

    @Override
    public void importDatabase() {
        backupCoordinator.importDatabase();
    }

    @Override
    public void openSupportLink() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException ex) {
            alert(getString(R.string.support_title), getString(R.string.support_open_failed));
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (memoryEditor.handleActivityResult(request, result, data)) {
            return;
        }
        backupCoordinator.handleActivityResult(request, result, data);
    }
}
