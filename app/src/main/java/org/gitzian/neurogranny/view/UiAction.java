package org.gitzian.neurogranny.view;

/** Typed user command emitted by the GL view. Never parse action names or IDs from strings. */
public final class UiAction {
    public enum Type {
        SELECT_MEMORY,
        BACK,
        ADD_MEMORY,
        OPEN_SETTINGS,
        SHOW_CUE,
        OPEN_REVIEW,
        OPEN_HISTORY,
        REVEAL_SECRET,
        TOGGLE_CHART,
        HISTORY_START,
        HISTORY_END,
        EDIT_MEMORY,
        EDITOR_SAVE,
        EDITOR_CANCEL,
        EDITOR_DELETE,
        IMPORT,
        EXPORT,
        SUPPORT_PROJECT,
        CHANGE_LANGUAGE
    }

    private final Type type;
    private final long memoryId;

    private UiAction(Type type, long memoryId) {
        this.type = type;
        this.memoryId = memoryId;
    }

    public static UiAction of(Type type) {
        if (type == Type.SELECT_MEMORY) {
            throw new IllegalArgumentException("A memory selection needs an ID");
        }
        return new UiAction(type, 0L);
    }

    public static UiAction selectMemory(long memoryId) {
        if (memoryId <= 0L) {
            throw new IllegalArgumentException("Memory ID must be positive");
        }
        return new UiAction(Type.SELECT_MEMORY, memoryId);
    }

    public Type type() {
        return type;
    }

    public long memoryId() {
        if (type != Type.SELECT_MEMORY) {
            throw new IllegalStateException("Action does not contain a memory ID");
        }
        return memoryId;
    }
}
