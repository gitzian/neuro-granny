package org.gitzian.neurogranny.model;

/** One failed attempt or one completed review. No secret material is stored. */
public final class ReviewEvent {
    public long id, memoryId, timestamp, intervalSeconds;
    public boolean correct;
    public int wrongAttempts;
    public String confidence = "";
}
