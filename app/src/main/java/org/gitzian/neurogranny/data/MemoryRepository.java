package org.gitzian.neurogranny.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import org.gitzian.neurogranny.model.CueMode;
import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ReviewEvent;
import org.gitzian.neurogranny.model.ScheduleMode;
import org.gitzian.neurogranny.model.SecretMode;

/** Central transaction boundary for reads/writes. IDs are LOCAL foreign keys, not merge identities. */
public final class MemoryRepository {
    // Projection order is intentionally identical to the order in read(Cursor).
    private static final String[] COLUMNS = {
            DatabaseContract.ID,
            DatabaseContract.TITLE,
            DatabaseContract.SECRET_MODE,
            DatabaseContract.PLAINTEXT,
            DatabaseContract.VERIFIER,
            DatabaseContract.SCHEDULE_MODE,
            DatabaseContract.FIXED_SECONDS,
            DatabaseContract.MIN_SECONDS,
            DatabaseContract.MAX_SECONDS,
            DatabaseContract.CURRENT_SECONDS,
            DatabaseContract.NEXT_DUE,
            DatabaseContract.DUE,
            DatabaseContract.PATTERN_SEED,
            DatabaseContract.PATTERN_VERSION,
            DatabaseContract.CUE_MODE,
            DatabaseContract.CUE_IMAGE,
            DatabaseContract.SUCCESSES,
            DatabaseContract.CREATED_AT,
            DatabaseContract.UPDATED_AT
    };

    private final NeuroDatabase helper;
    public MemoryRepository(NeuroDatabase helper) {
        this.helper = helper;
    }

    public NeuroDatabase helper() {
        return helper;
    }

    public static Memory read(Cursor c) {
        Memory m = new Memory();
        int i = 0;
        m.id = c.getLong(i++);
        m.title = c.getString(i++);
        m.mode = SecretMode.fromDatabaseValue(c.getInt(i++));
        m.plain = c.getString(i++);
        m.verifier = c.getString(i++);
        m.scheduleMode = ScheduleMode.fromDatabaseValue(c.getInt(i++));
        m.fixedSeconds = c.getLong(i++);
        m.minSeconds = c.getLong(i++);
        m.maxSeconds = c.getLong(i++);
        m.currentSeconds = c.getLong(i++);
        m.nextDue = c.getLong(i++);
        m.due = c.getInt(i++) != 0;
        m.patternSeed = c.getLong(i++);
        m.patternVersion = c.getInt(i++);
        m.cueMode = CueMode.fromDatabaseValue(c.getInt(i++));
        m.cueImage = c.isNull(i) ? null : c.getBlob(i);
        i++;
        m.successes = c.getInt(i++);
        m.createdAt = c.getLong(i++);
        m.updatedAt = c.getLong(i);
        return m;
    }

    public static String[] columns() {
        return COLUMNS.clone();
    }

    public static ContentValues values(Memory m) {
        ContentValues v = new ContentValues();
        v.put(DatabaseContract.TITLE, m.title);
        v.put(DatabaseContract.SECRET_MODE, m.mode.databaseValue());
        v.put(DatabaseContract.PLAINTEXT, m.plain);
        v.put(DatabaseContract.VERIFIER, m.verifier);
        v.put(DatabaseContract.SCHEDULE_MODE, m.scheduleMode.databaseValue());
        v.put(DatabaseContract.FIXED_SECONDS, m.fixedSeconds);
        v.put(DatabaseContract.MIN_SECONDS, m.minSeconds);
        v.put(DatabaseContract.MAX_SECONDS, m.maxSeconds);
        v.put(DatabaseContract.CURRENT_SECONDS, m.currentSeconds);
        v.put(DatabaseContract.NEXT_DUE, m.nextDue);
        v.put(DatabaseContract.DUE, m.due ? 1 : 0);
        v.put(DatabaseContract.PATTERN_SEED, m.patternSeed);
        v.put(DatabaseContract.PATTERN_VERSION, m.patternVersion);
        v.put(DatabaseContract.CUE_MODE, m.cueMode.databaseValue());
        v.put(DatabaseContract.CUE_IMAGE, m.cueImage);
        v.put(DatabaseContract.SUCCESSES, m.successes);
        v.put(DatabaseContract.CREATED_AT, m.createdAt);
        v.put(DatabaseContract.UPDATED_AT, m.updatedAt);
        return v;
    }

    public synchronized List<Memory> all() {
        List<Memory> result = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(DatabaseContract.MEMORIES, COLUMNS, null, null, null, null,
                DatabaseContract.TITLE + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) result.add(read(c));
        }
        return result;
    }

    public synchronized Memory get(long id) {
        try (Cursor c = helper.getReadableDatabase().query(DatabaseContract.MEMORIES, COLUMNS, DatabaseContract.ID + "=?", new String[] {
            String.valueOf(id)
        }, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    public synchronized long save(Memory memory) {
        return saveInternal(helper.getWritableDatabase(), memory);
    }

    /**
     * Changing a password creates a new learning identity. The new verifier,
     * schedule, pattern and zeroed statistics must be persisted together with
     * deletion of the old review events, or none of those changes may persist.
     */
    public synchronized long saveAfterPasswordChange(Memory memory) {
        if (memory.id <= 0) {
            throw new IllegalArgumentException("Password changes require an existing memory");
        }

        SQLiteDatabase database = helper.getWritableDatabase();
        database.beginTransaction();
        try {
            saveInternal(database, memory);
            database.delete(
                    DatabaseContract.REVIEW_EVENTS,
                    DatabaseContract.MEMORY_ID + "=?",
                    new String[] {String.valueOf(memory.id)});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return memory.id;
    }

    private static long saveInternal(SQLiteDatabase database, Memory memory) {
        memory.updatedAt = System.currentTimeMillis();
        if (memory.createdAt == 0) {
            memory.createdAt = memory.updatedAt;
        }

        if (memory.id == 0) {
            memory.id = database.insertOrThrow(
                    DatabaseContract.MEMORIES,
                    null,
                    values(memory));
        } else {
            int updatedRows = database.update(
                    DatabaseContract.MEMORIES,
                    values(memory),
                    DatabaseContract.ID + "=?",
                    new String[] {String.valueOf(memory.id)});
            if (updatedRows != 1) {
                throw new IllegalStateException("Missing memory");
            }
        }
        return memory.id;
    }

    public synchronized void delete(long id) {
        helper.getWritableDatabase().delete(DatabaseContract.MEMORIES, DatabaseContract.ID + "=?", new String[] {
            String.valueOf(id)
        });
    }

    public synchronized void setDue(long id) {
        ContentValues v = new ContentValues();
        v.put(DatabaseContract.DUE, 1);
        helper.getWritableDatabase().update(DatabaseContract.MEMORIES, v, DatabaseContract.ID + "=?", new String[] {
            String.valueOf(id)
        });
    }

    public synchronized List<ReviewEvent> history(long id, long start, long end) {
        List<ReviewEvent> result = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query(DatabaseContract.REVIEW_EVENTS, new String[] {
            DatabaseContract.ID, DatabaseContract.MEMORY_ID, DatabaseContract.TIMESTAMP, DatabaseContract.CORRECT, DatabaseContract.WRONG_ATTEMPTS, DatabaseContract.CONFIDENCE, DatabaseContract.INTERVAL_SECONDS
        }, DatabaseContract.MEMORY_ID + "=? AND " + DatabaseContract.TIMESTAMP + ">=? AND " + DatabaseContract.TIMESTAMP + "<=?", new String[] {
            String.valueOf(id), String.valueOf(start), String.valueOf(end)
        }, null, null, DatabaseContract.TIMESTAMP + " ASC")) {
            while (c.moveToNext()) {
                ReviewEvent e = new ReviewEvent();
                e.id = c.getLong(0);
                e.memoryId = c.getLong(1);
                e.timestamp = c.getLong(2);
                e.correct = c.getInt(3) != 0;
                e.wrongAttempts = c.getInt(4);
                e.confidence = c.getString(5);
                e.intervalSeconds = c.getLong(6);
                result.add(e);
            }
        }
        return result;
    }

    public synchronized void event(ReviewEvent e) {
        ContentValues v = new ContentValues();
        v.put(DatabaseContract.MEMORY_ID, e.memoryId);
        v.put(DatabaseContract.TIMESTAMP, e.timestamp);
        v.put(DatabaseContract.CORRECT, e.correct ? 1 : 0);
        v.put(DatabaseContract.WRONG_ATTEMPTS, e.wrongAttempts);
        v.put(DatabaseContract.CONFIDENCE, e.confidence);
        v.put(DatabaseContract.INTERVAL_SECONDS, e.intervalSeconds);
        helper.getWritableDatabase().insertOrThrow(DatabaseContract.REVIEW_EVENTS, null, v);
    }

    public synchronized void complete(Memory m, ReviewEvent e) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (db.update(DatabaseContract.MEMORIES, values(m), DatabaseContract.ID + "=?", new String[] {
                String.valueOf(m.id)
            }) != 1) throw new IllegalStateException("Missing memory");
            ContentValues v = new ContentValues();
            v.put(DatabaseContract.MEMORY_ID, e.memoryId);
            v.put(DatabaseContract.TIMESTAMP, e.timestamp);
            v.put(DatabaseContract.CORRECT, 1);
            v.put(DatabaseContract.WRONG_ATTEMPTS, e.wrongAttempts);
            v.put(DatabaseContract.CONFIDENCE, e.confidence);
            v.put(DatabaseContract.INTERVAL_SECONDS, e.intervalSeconds);
            db.insertOrThrow(DatabaseContract.REVIEW_EVENTS, null, v);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
