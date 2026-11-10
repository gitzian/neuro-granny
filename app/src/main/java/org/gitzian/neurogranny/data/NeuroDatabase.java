package org.gitzian.neurogranny.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Single schema version source of truth. Migrations MUST preserve records. */
public final class NeuroDatabase extends SQLiteOpenHelper {
    public static final String NAME = "neurogranny.sqlite";
    public static final int VERSION = 2;
    public NeuroDatabase(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE memories (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL COLLATE NOCASE,mode INTEGER NOT NULL,"
                + "plain TEXT NOT NULL DEFAULT '',verifier TEXT NOT NULL DEFAULT '',"
                + "schedule_mode INTEGER NOT NULL,fixed_seconds INTEGER NOT NULL,"
                + "min_seconds INTEGER NOT NULL,max_seconds INTEGER NOT NULL,"
                + "current_seconds INTEGER NOT NULL,next_due INTEGER NOT NULL,"
                + "is_due INTEGER NOT NULL DEFAULT 0,pattern_seed INTEGER NOT NULL,"
                + "pattern_version INTEGER NOT NULL DEFAULT 2,"
                + "cue_mode INTEGER NOT NULL DEFAULT 0,cue_image BLOB,"
                + "successes INTEGER NOT NULL DEFAULT 0,"
                + "created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,"
                + "CHECK ((mode=0 AND verifier='' AND length(plain)>0) OR "
                + "(mode=1 AND plain='' AND length(verifier)>0)))");
        db.execSQL("CREATE TABLE review_events (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "memory_id INTEGER NOT NULL REFERENCES memories(_id) ON DELETE CASCADE,"
                + "timestamp INTEGER NOT NULL,correct INTEGER NOT NULL,wrong_attempts INTEGER NOT NULL,"
                + "confidence TEXT NOT NULL DEFAULT '',interval_seconds INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX review_events_time ON review_events(memory_id,timestamp)");
        db.execSQL("CREATE INDEX memories_next_due ON memories(next_due)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE memories ADD COLUMN cue_mode INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE memories ADD COLUMN cue_image BLOB");
            oldVersion = 2;
        }
        if (oldVersion != newVersion) {
            throw new IllegalStateException("Missing database migration: " + oldVersion + " -> " + newVersion);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Downgrade unsupported: " + oldVersion + " -> " + newVersion);
    }

    /** Reject files that are not our schema, including unrelated SQLite files. */
    public static void verifyImport(SQLiteDatabase db) {
        int version = db.getVersion();
        if (version < 1 || version > VERSION) throw new IllegalArgumentException("Unsupported backup version: "
                + version);
        try (Cursor c = db.rawQuery("PRAGMA integrity_check", null)) {
            if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) throw new IllegalArgumentException("Database integrity check failed");
        }
        for (String table : new String[] {
            "memories", "review_events"
        }) {
            try (Cursor c = db.rawQuery("SELECT 1 FROM " + table + " LIMIT 0", null)) {
                /* existence */
            }
        }
        // Column access below rejects older/incompatible tables before mutations.
        try (Cursor c = db.rawQuery("SELECT _id,title,mode,plain,verifier,schedule_mode,pattern_seed FROM memories LIMIT 0", null)) {
        }
    }
}
