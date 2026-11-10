package org.gitzian.neurogranny.model;

import org.gitzian.neurogranny.patterns.PatternDesign;


/** Persisted memory item. Secret plaintext exists only when mode=RECOVERABLE. */
public final class Memory {
    public long id;
    public String title = "";
    public SecretMode mode = SecretMode.RECOVERABLE;
    public String plain = "";
    public String verifier = "";
    public ScheduleMode scheduleMode = ScheduleMode.AUTO_LEARNER;
    public long fixedSeconds = ReviewSettings.DEFAULT_FIXED_SECONDS;
    public long minSeconds = ReviewSettings.DEFAULT_MIN_SECONDS;
    public long maxSeconds = ReviewSettings.DEFAULT_MAX_SECONDS;
    public long currentSeconds = ReviewSettings.DEFAULT_MIN_SECONDS;
    public long nextDue;
    public boolean due;
    public long patternSeed;
    public int patternVersion = PatternDesign.FORMAT_VERSION;
    public CueMode cueMode = CueMode.GENERATED;
    public byte[] cueImage;
    public int successes;
    public long createdAt;
    public long updatedAt;
    public SecretMode secretMode() {
        return mode;
    }

    public ScheduleMode schedule() {
        return scheduleMode;
    }

    public Memory copy() {
        Memory m = new Memory();
        m.id = id;
        m.title = title;
        m.mode = mode;
        m.plain = plain;
        m.verifier = verifier;
        m.scheduleMode = scheduleMode;
        m.fixedSeconds = fixedSeconds;
        m.minSeconds = minSeconds;
        m.maxSeconds = maxSeconds;
        m.currentSeconds = currentSeconds;
        m.nextDue = nextDue;
        m.due = due;
        m.patternSeed = patternSeed;
        m.patternVersion = patternVersion;
        m.cueMode = cueMode;
        m.cueImage = cueImage == null ? null : cueImage.clone();
        m.successes = successes;
        m.createdAt = createdAt;
        m.updatedAt = updatedAt;
        return m;
    }
}
