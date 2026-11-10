package org.gitzian.neurogranny.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.backup.BackupService;
import org.gitzian.neurogranny.backup.TitleMatching;
import org.gitzian.neurogranny.controller.NeuroController;
import org.gitzian.neurogranny.data.MemoryRepository;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.SecretMode;
import org.gitzian.neurogranny.schedule.ReminderScheduler;

/** Manages manual SQLite export/import and title-conflict dialogs. */
final class BackupCoordinator {
    private static final int EXPORT_REQUEST = 1;
    private static final int IMPORT_REQUEST = 2;
    private static final String EXPORT_FILE_NAME = "neurogranny.sqlite";

    private final Activity activity;
    private final MemoryRepository repo;
    private final BackupService backups;
    private final ReminderScheduler scheduler;
    private final NeuroController controller;
    private final AppDialogs dialogs;

    BackupCoordinator(Activity activity, MemoryRepository repo, BackupService backups,
            ReminderScheduler scheduler, NeuroController controller, AppDialogs dialogs) {
        this.activity = activity;
        this.repo = repo;
        this.backups = backups;
        this.scheduler = scheduler;
        this.controller = controller;
        this.dialogs = dialogs;
    }

    void exportDatabase() {
        List<String> recoverable = new ArrayList<>();
        for (Memory memory : repo.all()) {
            if (memory.mode == SecretMode.RECOVERABLE) {
                recoverable.add(memory.title);
            }
        }

        String warning = activity.getString(R.string.export_unencrypted_warning) + "\n\n";
        if (recoverable.isEmpty()) {
            warning += activity.getString(R.string.export_no_plaintext);
        } else {
            warning += activity.getString(R.string.export_recoverable_list,
                    android.text.TextUtils.join("\n", recoverable));
        }
        dialogs.confirm(activity.getString(R.string.export_question), warning, () -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, EXPORT_FILE_NAME);
            activity.startActivityForResult(intent, EXPORT_REQUEST);
        });
    }

    void importDatabase() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        activity.startActivityForResult(i, IMPORT_REQUEST);
    }

    boolean handleActivityResult(int request, int result, Intent data) {
        if (request != EXPORT_REQUEST && request != IMPORT_REQUEST) {
            return false;
        }
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true;
        }
        Uri uri = data.getData();
        try {
            if (request == EXPORT_REQUEST) {
                try (OutputStream output = activity.getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) {
                        throw new IOException(activity.getString(R.string.cannot_write_file));
                    }
                    backups.exportTo(output);
                }
                dialogs.alert(activity.getString(R.string.export_complete),
                        activity.getString(R.string.export_complete_body));
            } else {
                BackupService.ImportSession session;
                try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                    if (input == null) {
                        throw new IOException(activity.getString(R.string.cannot_open_file));
                    }
                    session = backups.prepare(input);
                }
                reviewImport(session);
            }
        } catch (Exception exception) {
            int fallback = request == IMPORT_REQUEST ? R.string.invalid_backup_file
                    : R.string.cannot_write_file;
            dialogs.alert(activity.getString(R.string.file_operation_failed),
                    fileErrorMessage(exception, fallback));
        }
        return true;
    }

    private String fileErrorMessage(Exception exception, int fallbackResource) {
        if (exception instanceof BackupService.BackupException) {
            return activity.getString(((BackupService.BackupException) exception).messageResource());
        }
        return activity.getString(fallbackResource);
    }

    /** Select entries first, then choose per-title conflict behavior. Apply only once. */
    private void reviewImport(BackupService.ImportSession session) {
        List<BackupService.Entry> entries = session.entries;
        if (entries.isEmpty()) {
            session.close();
            dialogs.alert(activity.getString(R.string.empty_database), activity.getString(R.string.no_imported_memories));
            return;
        }
        String[] labels = new String[entries.size()];
        boolean[] enabled = new boolean[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            BackupService.Entry e = entries.get(i);
            int suffixId;
            switch (e.titleMatch) {
                case AMBIGUOUS:
                    suffixId = R.string.import_ambiguous_suffix;
                    break;
                case UNIQUE:
                    suffixId = R.string.import_conflict_suffix;
                    break;
                case NEW:
                default:
                    suffixId = R.string.import_new_suffix;
                    break;
            }
            labels[i] = e.incoming.title + activity.getString(suffixId);
            enabled[i] = e.titleMatch == TitleMatching.Kind.NEW;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.select_memories_to_import).setMultiChoiceItems(labels, enabled,
                (d, i, checked) -> enabled[i] = checked).setNegativeButton(R.string.cancel,(d, w) -> session.close())
                .setPositiveButton(R.string.continue_button,(d, w) -> resolveConflicts(session, enabled, 0))
                .setOnCancelListener(d -> session.close()).show();
    }

    private void resolveConflicts(BackupService.ImportSession session, boolean[] selected, int index) {
        if (index >= session.entries.size()) {
            int imports = 0;
            for (BackupService.Entry e : session.entries) if (e.choice != 0) imports++;
            final int total = imports;
            new AlertDialog.Builder(activity).setTitle(R.string.apply_import_title)
                    .setMessage(activity.getString(R.string.apply_import_message, total))
                    .setNegativeButton(R.string.cancel,(d, w) -> session.close())
                    .setOnCancelListener(d -> session.close()).setPositiveButton(R.string.import_action,(d, w) -> {
                try {
                    int count = backups.apply(session);
                    session.close();
                    scheduler.refresh();
                    controller.refresh();
                    dialogs.alert(activity.getString(R.string.import_complete), activity.getString(R.string.imported_count, count));
                } catch (Exception ex) {
                    session.close();
                    dialogs.alert(activity.getString(R.string.import_failed), activity.getString(R.string.import_failed_body));
                }
            }).show();
            return;
        }
        BackupService.Entry e = session.entries.get(index);
        if (!selected[index]) {
            e.choice = 0;
            resolveConflicts(session, selected, index + 1);
            return;
        }
        if (e.titleMatch == TitleMatching.Kind.AMBIGUOUS) {
            // Explicit selection imports separately; an ambiguous title never
            // identifies which existing record would be safe to overwrite.
            e.choice = 2;
            resolveConflicts(session, selected, index + 1);
            return;
        }
        if (e.titleMatch == TitleMatching.Kind.NEW) {
            e.choice = 1;
            resolveConflicts(session, selected, index + 1);
            return;
        }
        String comparison = e.sameSecret
                ? activity.getString(R.string.same_secret_summary)
                : activity.getString(R.string.secret_conflict_summary);
        String detail = comparison + "\n\n"
                + activity.getString(R.string.import_local_summary, modeName(e.local),
                        scheduleName(e.local), e.local.successes)
                + "\n" + activity.getString(R.string.import_incoming_summary,
                        modeName(e.incoming), scheduleName(e.incoming), e.incoming.successes)
                + "\n\n" + activity.getString(R.string.passwords_not_displayed);
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.import_conflict_title, e.incoming.title))
                .setMessage(detail)
                .setItems(new String[] {
                        activity.getString(R.string.keep_local),
                        activity.getString(R.string.use_imported),
                        activity.getString(R.string.keep_both)
                },(d, which) -> {
            e.choice = which;
            resolveConflicts(session, selected, index + 1);
        }).setOnCancelListener(d -> {
            session.close();
            dialogs.alert(activity.getString(R.string.import_canceled), activity.getString(R.string.no_changes_made));
        }).show();
    }

    private String modeName(Memory memory) {
        return activity.getString(memory.mode == SecretMode.RECOVERABLE
                ? R.string.recoverable : R.string.verify_only);
    }

    private String scheduleName(Memory memory) {
        switch (memory.schedule()) {
            case FIXED:
                return activity.getString(R.string.schedule_fixed);
            case AUTO_LEARNER:
                return activity.getString(R.string.schedule_auto);
            case ASK_NEXT_TIME:
                return activity.getString(R.string.schedule_ask_next);
            default:
                throw new IllegalStateException("Unknown scheduling mode");
        }
    }
}
