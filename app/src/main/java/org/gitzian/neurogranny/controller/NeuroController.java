package org.gitzian.neurogranny.controller;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.crypto.Secrets;
import org.gitzian.neurogranny.data.MemoryRepository;
import org.gitzian.neurogranny.model.Callback;
import org.gitzian.neurogranny.model.CueMode;
import org.gitzian.neurogranny.model.Interval;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ReviewEvent;
import org.gitzian.neurogranny.model.ReviewSettings;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;
import org.gitzian.neurogranny.patterns.PatternDesign;
import org.gitzian.neurogranny.patterns.PatternSeedSelector;
import org.gitzian.neurogranny.review.ReviewEngine;
import org.gitzian.neurogranny.schedule.ReminderScheduler;
import org.gitzian.neurogranny.view.UiAction;
import org.gitzian.neurogranny.view.UiScene;

/** Coordinates typed view actions, persistence, review policy and Android callbacks. */
public final class NeuroController {
    public interface Host {
        void render(UiScene scene);

        String text(int resourceId, Object... arguments);

        void chooseLanguage();
        void editor(Memory memory);
        void saveEditorForm();
        void requestEditPassword(Callback<String> callback);
        void alert(String title, String body);
        void confirm(String title, String body, Runnable yes);
        void confidence(Callback<Boolean> next);
        void nextInterval(Callback<Long> next);
        void chooseDate(long current, Callback<Long> next);
        void exportDatabase();
        void importDatabase();
        void openSupportLink();
        void busy(boolean value);
        void clearAnswer();
        void exitToSystem();
        boolean finishNotificationReview(long memoryId);
        void onUiThread(Runnable action);
    }

    private enum Purpose {
        REVIEW, CONVERT
    }

    private final MemoryRepository repo;
    private final ReminderScheduler alarms;
    private final Host host;
    private final ExecutorService crypto = Executors.newSingleThreadExecutor();
    private final SecureRandom rng = new SecureRandom();
    private UiScene.Page page = UiScene.Page.HOME;
    private long selectedId;
    private static final int MAX_TITLE_LENGTH = 120;

    private long from = System.currentTimeMillis()
            - ReviewSettings.HISTORY_WINDOW_DAYS * ReviewSettings.MILLIS_PER_DAY;
    private long to = System.currentTimeMillis();
    private boolean curve;
    private int scroll, wrongAttempts;
    private Purpose purpose = Purpose.REVIEW;
    private EditorDraft pending;
    private volatile boolean working;
    private UiScene renderedScene;
    public NeuroController(MemoryRepository repo, ReminderScheduler alarms, Host host) {
        this.repo = repo;
        this.alarms = alarms;
        this.host = host;
    }

    public long selectedMemoryId() {
        return selectedId;
    }

    public void shutdown() {
        crypto.shutdownNow();
    }

    public static final class EditorDraft {
        public String title;
        public String newSecret = "";
        public String repeatedSecret = "";
        public boolean regeneratePattern;
        public boolean cueEdited;
        public boolean hasPatternSeed;
        public long patternSeed;
        public SecretMode mode = SecretMode.RECOVERABLE;
        public ScheduleMode scheduleMode = ScheduleMode.AUTO_LEARNER;
        public CueMode cueMode = CueMode.GENERATED;
        public byte[] cueImage;
        public long fixedSeconds, minSeconds, maxSeconds;
    }

    public boolean openFromNotification(long id) {
        Memory m = repo.get(id);
        if (m == null) {
            return false;
        }
        selectedId = id;
        beginPrompt(Purpose.REVIEW);
        return true;
    }

    public void refresh() {
        Memory m = selectedId == 0 ? null : repo.get(selectedId);
        List<Memory> all = repo.all();
        List<ReviewEvent> events = selectedId == 0 ? Collections.emptyList() : repo.history(selectedId, from, to);
        renderedScene = new UiScene(page, all, m, events, from, to,
                curve, scroll, "", wrongAttempts);
        host.render(renderedScene);
    }

    public void scroll(int delta, int max) {
        int nextScroll = Math.max(0, Math.min(max, scroll + delta));
        if (nextScroll == scroll) {
            return;
        }
        scroll = nextScroll;
        if (renderedScene == null || renderedScene.page != page) {
            refresh();
        } else {
            // No SQLite query on each touch/animation frame.
            renderedScene = renderedScene.withScroll(scroll);
            host.render(renderedScene);
        }
    }

    public void act(UiAction action) {
        if (action.type() == UiAction.Type.SELECT_MEMORY) {
            selectedId = action.memoryId();
            if (repo.get(selectedId) == null) {
                return;
            }
            page = UiScene.Page.DETAILS;
            setHistoryDefaults();
            scroll = 0;
            refresh();
            return;
        }

        switch (action.type()) {
            case BACK:
                back();
                break;
            case ADD_MEMORY:
                newEditor();
                break;
            case OPEN_SETTINGS:
                page = UiScene.Page.SETTINGS;
                scroll = 0;
                refresh();
                break;
            case SHOW_CUE:
                page = UiScene.Page.CUE;
                scroll = 0;
                refresh();
                break;
            case OPEN_REVIEW:
                beginPrompt(Purpose.REVIEW);
                break;
            case OPEN_HISTORY:
                page = UiScene.Page.HISTORY;
                scroll = 0;
                setHistoryDefaults();
                refresh();
                break;
            case REVEAL_SECRET:
                Memory secretMemory = repo.get(selectedId);
                if (secretMemory != null && secretMemory.mode == SecretMode.RECOVERABLE) {
                    host.alert(host.text(R.string.stored_password), secretMemory.plain);
                }
                break;
            case TOGGLE_CHART:
                curve = !curve;
                refresh();
                break;
            case HISTORY_START:
                host.chooseDate(from, value -> {
                    from = value;
                    if (from > to) {
                        to = from;
                    }
                    refresh();
                });
                break;
            case HISTORY_END:
                host.chooseDate(to, value -> {
                    to = value;
                    if (to < from) {
                        from = to;
                    }
                    refresh();
                });
                break;
            case EDIT_MEMORY:
                Memory memory = repo.get(selectedId);
                if (memory != null) {
                    page = UiScene.Page.EDITOR;
                    scroll = 0;
                    refresh();
                    host.editor(memory);
                }
                break;
            case EDITOR_SAVE:
                host.saveEditorForm();
                break;
            case EDITOR_CANCEL:
                back();
                break;
            case EDITOR_DELETE:
                deleteSelected();
                break;
            case IMPORT:
                host.importDatabase();
                break;
            case EXPORT:
                host.exportDatabase();
                break;
            case SUPPORT_PROJECT:
                host.openSupportLink();
                break;
            case CHANGE_LANGUAGE:
                host.chooseLanguage();
                break;
            default:
                throw new IllegalStateException("Unhandled user action: " + action.type());
        }
    }

    public void back() {
        switch (page) {
        case HOME:
            host.exitToSystem();
            return;
        case PROMPT:
            pending = null;
            wrongAttempts = 0;
            page = selectedId == 0 ? UiScene.Page.HOME : UiScene.Page.DETAILS;
            break;
        case CUE:
        case HISTORY:
            page = UiScene.Page.DETAILS;
            break;
        case EDITOR:
            page = selectedId == 0 ? UiScene.Page.HOME : UiScene.Page.DETAILS;
            break;
        case DETAILS:
        case SETTINGS:
            page = UiScene.Page.HOME;
            break;
        }
        scroll = 0;
        refresh();
    }

    private void setHistoryDefaults() {
        to = System.currentTimeMillis();
        from = ReviewSettings.historyWindowStart(to);
    }

    private void beginPrompt(Purpose p) {
        purpose = p;
        wrongAttempts = 0;
        page = UiScene.Page.PROMPT;
        scroll = 0;
        refresh();
        host.clearAnswer();
    }

    /** Called by MainActivity on the UI thread. Argon2id is performed off-thread. */
    public void submitAnswer(String answer) {
        if (working || page != UiScene.Page.PROMPT) return;
        final Memory m = repo.get(selectedId);
        if (m == null) {
            back();
            return;
        }
        final Purpose submittedPurpose = purpose;
        working = true;
        host.busy(true);
        crypto.execute(() -> {
            char[] pass = answer.toCharArray();
            boolean success;
            try {
                success = Secrets.verify(m, pass);
            } finally {
                Arrays.fill(pass, '\0');
            }
            final boolean ok = success;
            // Host.render is always executed by MainActivity on its UI thread.
            host.onUiThread(() -> {
                working = false;
                host.busy(false);
                if (page != UiScene.Page.PROMPT
                        || submittedPurpose != purpose
                        || selectedId != m.id) {
                    return;
                }
                host.clearAnswer();
                if (!ok) {
                    if (purpose == Purpose.REVIEW) {
                        wrongAttempts++;
                        ReviewEvent e = new ReviewEvent();
                        e.memoryId = m.id;
                        e.timestamp = System.currentTimeMillis();
                        e.correct = false;
                        e.wrongAttempts = wrongAttempts;
                        e.intervalSeconds = m.currentSeconds;
                        repo.event(e);
                    }
                    refresh();
                    if (purpose == Purpose.REVIEW && m.mode == SecretMode.RECOVERABLE) {
                        host.confirm(host.text(R.string.incorrect_title), host.text(R.string.incorrect_reveal_message),
                                () -> host.alert(host.text(R.string.stored_password), m.plain));
                    } else {
                        host.alert(host.text(R.string.incorrect_title),
                                purpose == Purpose.REVIEW
                                        ? host.text(R.string.incorrect_retry_message)
                                        : host.text(R.string.password_not_matched));
                    }
                    return;
                }
                switch (submittedPurpose) {
                case CONVERT:
                    if (pending == null) {
                        page = UiScene.Page.DETAILS;
                        refresh();
                        break;
                    }
                    EditorDraft toConvert = pending;
                    pending = null;
                    applyEditor(toConvert, answer);
                    break;
                case REVIEW:
                    finishReview(m);
                    break;
                }
            });
        });
    }

    private void finishReview(Memory original) {
        final int failures = wrongAttempts;
        if (original.scheduleMode == ScheduleMode.AUTO_LEARNER && ReviewEngine.askConfidence(original, failures)) {
            host.confidence(easy -> commitReview(original, failures, easy, 0));
        } else if (original.scheduleMode == ScheduleMode.ASK_NEXT_TIME) {
            host.nextInterval(seconds -> commitReview(original, failures, false, seconds));
        } else commitReview(original, failures, false, 0);
    }

    private void commitReview(Memory original, int failures, boolean easy, long askedSeconds) {
        Memory m = repo.get(original.id);
        if (m == null) return;
        long next = ReviewEngine.afterSuccess(m, failures, easy, askedSeconds);
        long now = System.currentTimeMillis();
        m.currentSeconds = next;
        m.nextDue = now + next * 1000L;
        m.due = false;
        m.successes++;
        m.updatedAt = now;
        ReviewEvent e = new ReviewEvent();
        e.memoryId = m.id;
        e.timestamp = now;
        e.correct = true;
        e.wrongAttempts = failures;
        e.confidence = failures >= 2 ? "failed-twice" : (m.scheduleMode == ScheduleMode.AUTO_LEARNER ? (easy ? "easy" : "hard") : "");
        e.intervalSeconds = next;
        repo.complete(m, e);
        alarms.cancelMemory(m.id);
        alarms.refresh();
        // Notification reviews are complete only after the result and schedule are saved.
        // Close the reminder-only flow instead of navigating into the app.
        if (host.finishNotificationReview(m.id)) {
            return;
        }
        page = UiScene.Page.DETAILS;
        wrongAttempts = 0;
        setHistoryDefaults();
        refresh();
        if (ReviewEngine.offerGraduation(m, failures)) {
            host.confirm(host.text(R.string.graduation_title),
                    host.text(R.string.graduation_message), () -> {
                EditorDraft d = new EditorDraft();
                d.title = m.title;
                d.mode = SecretMode.VERIFY_ONLY;
                d.scheduleMode = m.scheduleMode;
                d.fixedSeconds = m.fixedSeconds;
                d.minSeconds = m.minSeconds;
                d.maxSeconds = m.maxSeconds;
                pending = d;
                beginPrompt(Purpose.CONVERT);
            });
        }
    }

    /**
     * Save from the visible editor. An existing memorized secret is verified only
     * AFTER Save is pressed; failed verification leaves the editor and its fields.
     */
    public void saveEditor(EditorDraft draft, Callback<Boolean> completed) {
        if (draft == null) {
            completed.accept(false);
            return;
        }
        if (draft.title == null || draft.title.trim().isEmpty()
                || draft.title.trim().length() > MAX_TITLE_LENGTH) {
            host.alert(host.text(R.string.invalid_name), host.text(R.string.name_length_error, MAX_TITLE_LENGTH));
            completed.accept(false);
            return;
        }
        if (draft.fixedSeconds < 1 || draft.minSeconds < 1
                || draft.maxSeconds < draft.minSeconds
                || draft.maxSeconds > Interval.MAX_INTERVAL_SECONDS) {
            host.alert(host.text(R.string.invalid_interval), host.text(R.string.interval_bounds_error));
            completed.accept(false);
            return;
        }
        if (draft.cueEdited && draft.cueMode == CueMode.PHOTO
                && (draft.cueImage == null || draft.cueImage.length == 0)) {
            host.alert(host.text(R.string.invalid_entry), host.text(R.string.choose_custom_photo));
            completed.accept(false);
            return;
        }
        boolean secretChanged = draft.newSecret != null && !draft.newSecret.isEmpty();
        if (secretChanged && !draft.newSecret.equals(draft.repeatedSecret)) {
            host.alert(host.text(R.string.passwords_differ), host.text(R.string.passwords_must_match));
            completed.accept(false);
            return;
        }

        Memory existing = selectedId == 0 ? null : repo.get(selectedId);
        if (existing == null) {
            if (!secretChanged) {
                host.alert(host.text(R.string.missing_password), host.text(R.string.enter_repeat_password));
                completed.accept(false);
                return;
            }
            completed.accept(applyEditor(draft, null));
            return;
        }

        Runnable proceed = () -> {
            // Recoverable entries with only metadata edits need no password gate.
            boolean requiresPassword = existing.mode == SecretMode.VERIFY_ONLY
                    || draft.mode != existing.mode || secretChanged;
            if (!requiresPassword) {
                completed.accept(applyEditor(draft, null));
            } else {
                host.requestEditPassword(entered -> verifyAndSave(existing.id, draft,
                        entered, completed));
            }
        };
        Runnable confirmPasswordChanges = () -> {
            if (existing.mode == SecretMode.RECOVERABLE
                    && draft.mode == SecretMode.VERIFY_ONLY) {
                host.confirm(host.text(R.string.convert_title),
                        host.text(R.string.convert_message), proceed);
            } else if (secretChanged) {
                host.confirm(host.text(R.string.change_password_title),
                        host.text(R.string.change_password_message), proceed);
            } else {
                proceed.run();
            }
        };

        // The editor confirms regeneration when the user presses its contextual button.
        confirmPasswordChanges.run();
    }

    private void verifyAndSave(long expectedId, EditorDraft draft,
            String entered, Callback<Boolean> completed) {
        if (entered == null) {
            completed.accept(false);
            return;
        }
        Memory existing = repo.get(expectedId);
        if (existing == null || working) {
            completed.accept(false);
            return;
        }
        working = true;
        crypto.execute(() -> {
            char[] password = entered.toCharArray();
            boolean verified;
            try {
                verified = Secrets.verify(existing, password);
            } finally {
                Arrays.fill(password, '\0');
            }
            host.onUiThread(() -> {
                working = false;
                if (!verified) {
                    host.alert(host.text(R.string.incorrect_password), host.text(R.string.nothing_saved));
                    completed.accept(false);
                    return;
                }
                if (selectedId != expectedId || page != UiScene.Page.EDITOR) {
                    completed.accept(false);
                    return;
                }
                completed.accept(applyEditor(draft, entered));
            });
        });
    }

    /** matchedCurrentPassword exists only while returning from a password prompt. */
    private boolean applyEditor(EditorDraft d, String matchedCurrentPassword) {
        Memory m = selectedId == 0 ? null : repo.get(selectedId);
        if (m == null) m = new Memory();
        boolean isNew = m.id == 0;
        boolean changedSecret = !isNew && !d.newSecret.isEmpty();
        m.title = d.title.trim();
        m.scheduleMode = d.scheduleMode;
        m.fixedSeconds = d.fixedSeconds;
        m.minSeconds = d.minSeconds;
        m.maxSeconds = d.maxSeconds;
        if (isNew || changedSecret || d.mode != m.mode) {
            String value = !d.newSecret.isEmpty() ? d.newSecret : matchedCurrentPassword;
            if (value == null || value.isEmpty()) {
                host.alert(host.text(R.string.missing_password), host.text(R.string.correct_password_required));
                return false;
            }
            char[] pass = value.toCharArray();
            try {
                Secrets.setSecret(m, pass, d.mode);
            } finally {
                Arrays.fill(pass, '\0');
            }
            if (isNew || changedSecret) {
                m.patternSeed = d.hasPatternSeed ? d.patternSeed : createDistinctPatternSeed();
                m.patternVersion = PatternDesign.FORMAT_VERSION;
                m.successes = 0;
                m.currentSeconds = m.scheduleMode == ScheduleMode.AUTO_LEARNER ? m.minSeconds : m.fixedSeconds;
                m.nextDue = System.currentTimeMillis() + m.currentSeconds * 1000L;
                m.due = false;
            }
        }

        if (d.cueEdited) {
            m.cueMode = d.cueMode;
            // Retain an unused photo when switching to a pattern; switching back is reversible.
            m.cueImage = d.cueImage == null ? null : d.cueImage.clone();
        }

        // A cue-only change does not replace the secret or reset review history.
        if (!isNew && !changedSecret && d.regeneratePattern) {
            m.patternSeed = d.hasPatternSeed ? d.patternSeed : createDistinctPatternSeed();
            m.patternVersion = PatternDesign.FORMAT_VERSION;
        }

        if (m.cueMode == CueMode.PHOTO && (m.cueImage == null || m.cueImage.length == 0)) {
            host.alert(host.text(R.string.invalid_entry), host.text(R.string.choose_custom_photo));
            return false;
        }

        if (isNew) {
            if (m.scheduleMode == ScheduleMode.ASK_NEXT_TIME) m.currentSeconds = m.fixedSeconds;
            m.nextDue = System.currentTimeMillis() + m.currentSeconds * 1000L;
        }
        // If a schedule is changed, make the next due reflect the new settings.
        if (!isNew && !m.due && m.scheduleMode == ScheduleMode.FIXED && m.currentSeconds != m.fixedSeconds) {
            m.currentSeconds = m.fixedSeconds;
            m.nextDue = System.currentTimeMillis() + m.currentSeconds * 1000L;
        }
        if (changedSecret) {
            repo.saveAfterPasswordChange(m);
        } else {
            repo.save(m);
        }
        selectedId = m.id;
        page = UiScene.Page.DETAILS;
        setHistoryDefaults();
        pending = null;
        alarms.refresh();
        scroll = 0;
        refresh();
        return true;
    }

    /**
     * Evaluate independent random designs against all assigned visual cues.
     * No existing pattern changes during cue creation or regeneration.
     */
    private long createDistinctPatternSeed() {
        return previewPatternSeed(0L);
    }

    /** Candidate selection never mutates a stored pattern. */
    public long previewPatternSeed(long excludeSeed) {
        List<Long> existingSeeds = new java.util.ArrayList<>();
        for (Memory existing : repo.all()) {
            existingSeeds.add(existing.patternSeed);
        }
        existingSeeds.add(excludeSeed);
        long seed;
        do {
            seed = PatternSeedSelector.choose(rng, existingSeeds);
        } while (seed == excludeSeed);
        return seed;
    }

    public void deleteSelected() {
        Memory m = repo.get(selectedId);
        if (m == null) return;
        host.confirm(host.text(R.string.delete_title),
                host.text(R.string.delete_message, m.title), () -> {
            repo.delete(m.id);
            alarms.cancelMemory(m.id);
            alarms.refresh();
            selectedId = 0;
            page = UiScene.Page.HOME;
            scroll = 0;
            refresh();
        });
    }

    /** Called before displaying a NEW editor, otherwise selectedId can refer to a previous item. */
    public void newEditor() {
        selectedId = 0;
        page = UiScene.Page.EDITOR;
        scroll = 0;
        refresh();
        host.editor(null);
    }
}
