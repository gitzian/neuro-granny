package org.gitzian.neurogranny.backup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gitzian.neurogranny.R;
import org.gitzian.neurogranny.data.DatabaseContract;
import org.gitzian.neurogranny.data.MemoryRepository;
import org.gitzian.neurogranny.data.NeuroDatabase;
import org.gitzian.neurogranny.model.CueMode;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.SecretMode;
import org.gitzian.neurogranny.patterns.PatternDesign;

/**
 * Portable, intentionally UNENCRYPTED SQLite backups.
 * Import validates in private temporary storage before touching the live database.
 * Caller explicitly resolves same-title conflicts. Never execute SQL from an import.
 */
public final class BackupService {
    public static final long MAX_IMPORT_BYTES = 50L * 1024 * 1024;
    private static final String EXPORT_TEMP_FILE_PREFIX = "neurogranny-export-";

    /** Identifies an expected backup error without exposing internal text in dialogs. */
    public static final class BackupException extends IOException {
        private final int messageResource;

        public BackupException(int messageResource) {
            super();
            this.messageResource = messageResource;
        }

        public BackupException(int messageResource, Throwable cause) {
            super(cause);
            this.messageResource = messageResource;
        }

        public int messageResource() {
            return messageResource;
        }
    }

    private final Context context;
    private final MemoryRepository repo;
    public BackupService(Context context, MemoryRepository repo) {
        this.context = context;
        this.repo = repo;
    }

    public synchronized void exportTo(OutputStream output) throws IOException {
        // Export from one coherent database transaction into a freshly created standard
        // SQLite file. No dependency on SQLite's version-specific VACUUM INTO or backup API.
        // Source database remains open; a concurrent reminder cannot corrupt the snapshot.
        File tmp = File.createTempFile(EXPORT_TEMP_FILE_PREFIX, ".sqlite", context.getCacheDir());
        SQLiteDatabase source = repo.helper().getReadableDatabase();
        SQLiteDatabase target = null;
        try {
            target = SQLiteDatabase.openOrCreateDatabase(tmp, null);
            target.setForeignKeyConstraintsEnabled(true);
            source.beginTransactionNonExclusive();
            try {
                target.beginTransaction();
                try {
                    repo.helper().onCreate(target);
                    copyTable(source, target, DatabaseContract.MEMORIES);
                    copyTable(source, target, DatabaseContract.REVIEW_EVENTS);
                    target.setVersion(NeuroDatabase.VERSION);
                    target.setTransactionSuccessful();
                } finally {
                    target.endTransaction();
                }
                source.setTransactionSuccessful();
            } finally {
                source.endTransaction();
            }
            target.close();
            target = null;
            try (InputStream input = new FileInputStream(tmp)) {
                copy(input, output, MAX_IMPORT_BYTES);
                output.flush();
            }
        } finally {
            if (target != null) target.close();
            if (!tmp.delete()) tmp.deleteOnExit();
        }
    }

    private static void copyTable(SQLiteDatabase source, SQLiteDatabase target, String table) {
        try (Cursor cursor = source.rawQuery("SELECT * FROM " + table, null)) {
            while (cursor.moveToNext()) {
                ContentValues row = new ContentValues();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String name = cursor.getColumnName(i);
                    switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_NULL:
                        row.putNull(name);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        row.put(name, cursor.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        row.put(name, cursor.getDouble(i));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        row.put(name, cursor.getBlob(i));
                        break;
                        default : row.put(name, cursor.getString(i));
                        break;
                    }
                }
                target.insertOrThrow(table, null, row);
            }
        }
    }

    public static final class Entry {
        public Memory incoming;
        public Memory local;
        public TitleMatching.Kind titleMatch;
        public boolean sameVerifier;
        public boolean sameSecret;

        /**
         * 0 = skip, 1 = import/replace an unambiguous match, 2 = import
         * separately using a distinct title. Ambiguous matches cannot replace.
         */
        public int choice;
    }

    public static final class ImportSession implements AutoCloseable {
        public final List<Entry> entries;
        private final SQLiteDatabase database;
        private final File file;
        private ImportSession(SQLiteDatabase db, File file, List<Entry> entries) {
            this.database = db;
            this.file = file;
            this.entries = entries;
        }
        @Override
        public void close() {
            database.close();
            if (!file.delete()) file.deleteOnExit();
        }
    }

    public ImportSession prepare(InputStream stream) throws IOException {
        File tmp = File.createTempFile("neurogranny-import-", ".sqlite", context.getCacheDir());
        SQLiteDatabase incoming = null;
        try {
            try (OutputStream output = new FileOutputStream(tmp)) {
                copy(stream, output, MAX_IMPORT_BYTES);
            }
            incoming = SQLiteDatabase.openDatabase(tmp.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            NeuroDatabase.verifyImport(incoming);
            Map<String, List<Memory>> localByTitle = new HashMap<>();
            for (Memory local : repo.all()) {
                String titleKey = TitleMatching.normalize(local.title);
                List<Memory> matchingMemories = localByTitle.get(titleKey);
                if (matchingMemories == null) {
                    matchingMemories = new ArrayList<>();
                    localByTitle.put(titleKey, matchingMemories);
                }
                matchingMemories.add(local);
            }

            List<Memory> importedMemories = new ArrayList<>();
            Map<String, Integer> incomingTitleCounts = new HashMap<>();
            try (Cursor cursor = incoming.query(
                    DatabaseContract.MEMORIES,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DatabaseContract.TITLE + " COLLATE NOCASE ASC")) {
                while (cursor.moveToNext()) {
                    Memory memory = readImportedMemory(cursor);
                    validateImportedMemory(memory);
                    importedMemories.add(memory);
                    String titleKey = TitleMatching.normalize(memory.title);
                    Integer previousCount = incomingTitleCounts.get(titleKey);
                    incomingTitleCounts.put(
                            titleKey,
                            previousCount == null ? 1 : previousCount + 1);
                }
            }

            List<Entry> entries = new ArrayList<>();
            for (Memory memory : importedMemories) {
                String titleKey = TitleMatching.normalize(memory.title);
                List<Memory> localMatches = localByTitle.get(titleKey);
                int localCount = localMatches == null ? 0 : localMatches.size();
                int incomingCount = incomingTitleCounts.get(titleKey);

                Entry entry = new Entry();
                entry.incoming = memory;
                entry.titleMatch = TitleMatching.classify(localCount, incomingCount);
                if (entry.titleMatch == TitleMatching.Kind.AMBIGUOUS) {
                    // Multiple equally named records cannot be matched safely.
                    // Allow an explicit separate import, never arbitrary replace.
                    entry.choice = 0;
                } else if (entry.titleMatch == TitleMatching.Kind.UNIQUE) {
                    entry.local = localMatches.get(0);
                    entry.sameVerifier = memory.mode == SecretMode.VERIFY_ONLY
                            && entry.local.mode == SecretMode.VERIFY_ONLY
                            && memory.verifier.equals(entry.local.verifier);
                    entry.sameSecret = entry.sameVerifier
                            || (memory.mode == SecretMode.RECOVERABLE
                            && entry.local.mode == SecretMode.RECOVERABLE
                            && memory.plain.equals(entry.local.plain));
                    entry.choice = 0;
                } else {
                    entry.choice = 1;
                }
                entries.add(entry);
            }
            return new ImportSession(incoming, tmp, entries);
        } catch (Exception ex) {
            if (incoming != null) incoming.close();
            if (!tmp.delete()) tmp.deleteOnExit();
            if (ex instanceof BackupException) {
                throw (BackupException) ex;
            }
            throw new BackupException(R.string.invalid_backup_file, ex);
        }
    }

    private static Memory readImportedMemory(Cursor cursor) {
        Memory memory = new Memory();
        memory.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ID));
        memory.title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.TITLE));
        memory.mode = SecretMode.fromDatabaseValue(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SECRET_MODE)));
        memory.plain = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PLAINTEXT));
        memory.verifier = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.VERIFIER));
        memory.scheduleMode = org.gitzian.neurogranny.model.ScheduleMode.fromDatabaseValue(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SCHEDULE_MODE)));
        memory.fixedSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FIXED_SECONDS));
        memory.minSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.MIN_SECONDS));
        memory.maxSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.MAX_SECONDS));
        memory.currentSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.CURRENT_SECONDS));
        memory.nextDue = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.NEXT_DUE));
        memory.due = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.DUE)) != 0;
        memory.patternSeed = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.PATTERN_SEED));
        memory.patternVersion = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.PATTERN_VERSION));
        int cueModeColumn = cursor.getColumnIndex(DatabaseContract.CUE_MODE);
        if (cueModeColumn >= 0) {
            memory.cueMode = CueMode.fromDatabaseValue(cursor.getInt(cueModeColumn));
        }
        int cueImageColumn = cursor.getColumnIndex(DatabaseContract.CUE_IMAGE);
        if (cueImageColumn >= 0 && !cursor.isNull(cueImageColumn)) {
            memory.cueImage = cursor.getBlob(cueImageColumn);
        }
        memory.successes = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SUCCESSES));
        memory.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.CREATED_AT));
        memory.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.UPDATED_AT));
        return memory;
    }

    private static void validateImportedMemory(Memory memory) throws IOException {
        if (memory.title == null || memory.title.trim().isEmpty()
                || (memory.mode == SecretMode.RECOVERABLE
                && (memory.plain == null || memory.plain.isEmpty()))
                || (memory.mode == SecretMode.VERIFY_ONLY
                && (memory.verifier == null || memory.verifier.isEmpty()))
                || memory.minSeconds < 1
                || memory.maxSeconds < memory.minSeconds
                || memory.fixedSeconds < 1
                || memory.currentSeconds < 1
                || memory.patternVersion != PatternDesign.FORMAT_VERSION
                || (memory.cueMode == CueMode.PHOTO
                && (memory.cueImage == null || memory.cueImage.length == 0))) {
            throw new BackupException(R.string.invalid_backup_record);
        }
    }

    public synchronized int apply(ImportSession session) {
        SQLiteDatabase db = repo.helper().getWritableDatabase();
        int imported = 0;
        db.beginTransaction();
        try {
            for (Entry e : session.entries) {
                if (e.choice == 0) {
                    continue;
                }
                if (e.choice != 1 && e.choice != 2) {
                    throw new IllegalArgumentException("Invalid import choice");
                }
                if (e.titleMatch == TitleMatching.Kind.AMBIGUOUS && e.choice != 2) {
                    throw new IllegalArgumentException("Ambiguous titles cannot replace records");
                }
                if (e.choice == 1 && e.local != null) {
                    // Only a uniquely matched local record can be replaced.
                    if (e.titleMatch != TitleMatching.Kind.UNIQUE) {
                        throw new IllegalArgumentException("Unsafe replacement target");
                    }
                    int deletedRows = db.delete(
                            DatabaseContract.MEMORIES,
                            DatabaseContract.ID + "=?",
                            new String[] {String.valueOf(e.local.id)});
                    if (deletedRows != 1) {
                        throw new IllegalStateException("Import target no longer exists");
                    }
                }
                Memory item = e.incoming.copy();
                long sourceId = item.id;
                item.id = 0;
                if (e.choice == 2) {
                    item.title = distinctTitle(db, item.title);
                }
                long localId = db.insertOrThrow(DatabaseContract.MEMORIES, null, MemoryRepository.values(item));
                try (Cursor c = session.database.query(DatabaseContract.REVIEW_EVENTS, new String[] {
                    DatabaseContract.TIMESTAMP,
                        DatabaseContract.CORRECT,
                        DatabaseContract.WRONG_ATTEMPTS,
                        DatabaseContract.CONFIDENCE,
                        DatabaseContract.INTERVAL_SECONDS
                }, DatabaseContract.MEMORY_ID + "=?", new String[] {
                    String.valueOf(sourceId)
                }, null, null, DatabaseContract.TIMESTAMP + " ASC")) {
                    while (c.moveToNext()) {
                        ContentValues v = new ContentValues();
                        v.put(DatabaseContract.MEMORY_ID, localId);
                        v.put(DatabaseContract.TIMESTAMP, c.getLong(0));
                        v.put(DatabaseContract.CORRECT, c.getInt(1));
                        v.put(DatabaseContract.WRONG_ATTEMPTS, c.getInt(2));
                        v.put(DatabaseContract.CONFIDENCE, c.getString(3));
                        v.put(DatabaseContract.INTERVAL_SECONDS, c.getLong(4));
                        db.insertOrThrow(DatabaseContract.REVIEW_EVENTS, null, v);
                    }
                }
                imported++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return imported;
    }

    private String distinctTitle(SQLiteDatabase db, String base) {
        int index = 2;
        String name = base + context.getString(R.string.imported_suffix);
        while (true) {
            try (Cursor c = db.rawQuery("SELECT 1 FROM memories WHERE title=? COLLATE NOCASE LIMIT 1", new String[] {
                name
            })) {
                if (!c.moveToNext()) return name;
            }
            name = base + context.getString(R.string.imported_suffix_number, index++);
        }
    }

    private static void copy(InputStream in, OutputStream out, long max) throws IOException {
        byte[] buf = new byte[32768];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != - 1) {
            total += n;
            if (total > max) {
                throw new BackupException(R.string.backup_too_large);
            }
            out.write(buf, 0, n);
        }
    }
}
