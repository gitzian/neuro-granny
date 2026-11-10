package org.gitzian.neurogranny.view;

import org.gitzian.neurogranny.model.Memory;
import org.gitzian.neurogranny.model.ReviewEvent;
import java.util.*;

/** Immutable-to-the-renderer snapshot: no database, no passwords needed for painting. */
public final class UiScene {
    public enum Page {
        HOME, DETAILS, EDITOR, CUE, PROMPT, HISTORY, SETTINGS
    }

    public final Page page;
    public final List<Memory> memories;
    public final Memory current;
    public final List<ReviewEvent> history;
    public final long historyFrom, historyTo;
    public final boolean curve;
    public final int listScroll;
    public final String promptTitle;
    public final int wrongAttempts;
    public UiScene(Page page, List<Memory> memories, Memory current, List<ReviewEvent> history, long from,
            long to, boolean curve, int listScroll, String promptTitle, int wrongAttempts) {
        this.page = page;
        // The renderer only reads a memory's display fields. Never carry plaintext/verifiers into snapshots.
        ArrayList<Memory> sanitized = new ArrayList<>();
        for (Memory m : memories) sanitized.add(displayOnly(m));
        this.memories = Collections.unmodifiableList(sanitized);
        this.current = current == null ? null : displayOnly(current);
        this.history = Collections.unmodifiableList(new ArrayList<>(history));
        this.historyFrom = from;
        this.historyTo = to;
        this.curve = curve;
        this.listScroll = listScroll;
        this.promptTitle = promptTitle;
        this.wrongAttempts = wrongAttempts;
    }

    /** Reuses the existing immutable data while the user drags the content. */
    private UiScene(UiScene existing, int newScroll) {
        this.page = existing.page;
        this.memories = existing.memories;
        this.current = existing.current;
        this.history = existing.history;
        this.historyFrom = existing.historyFrom;
        this.historyTo = existing.historyTo;
        this.curve = existing.curve;
        this.listScroll = newScroll;
        this.promptTitle = existing.promptTitle;
        this.wrongAttempts = existing.wrongAttempts;
    }

    public UiScene withScroll(int newScroll) {
        return new UiScene(this, newScroll);
    }

    private static Memory displayOnly(Memory m) {
        Memory o = m.copy();
        o.plain = "";
        o.verifier = "";
        return o;
    }
}
