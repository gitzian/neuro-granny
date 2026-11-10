package org.gitzian.neurogranny.backup;

import java.util.Locale;

/**
 * Conservative title matching for imported memories. A title is only a
 * replacement candidate when unique in both the active and imported sets.
 * Title equality does not prove that the underlying passwords are identical.
 */
public final class TitleMatching {

    public enum Kind {
        NEW,
        UNIQUE,
        AMBIGUOUS
    }

    private TitleMatching() {
    }

    public static String normalize(String title) {
        return title.trim().toLowerCase(Locale.ROOT);
    }

    public static Kind classify(int localCount, int importedCount) {
        if (localCount < 0 || importedCount < 1) {
            throw new IllegalArgumentException("Invalid title counts");
        }
        if (localCount > 1 || importedCount > 1) {
            return Kind.AMBIGUOUS;
        }
        return localCount == 1 ? Kind.UNIQUE : Kind.NEW;
    }
}
