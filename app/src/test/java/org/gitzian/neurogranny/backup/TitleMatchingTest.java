package org.gitzian.neurogranny.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class TitleMatchingTest {

    @Test
    public void noLocalTitleIsNew() {
        assertEquals(TitleMatching.Kind.NEW, TitleMatching.classify(0, 1));
    }

    @Test
    public void uniqueTitleIsEligibleForExplicitReplacement() {
        assertEquals(TitleMatching.Kind.UNIQUE, TitleMatching.classify(1, 1));
    }

    @Test
    public void twoLocalMatchesMustNotAutomaticallyReplace() {
        assertEquals(TitleMatching.Kind.AMBIGUOUS, TitleMatching.classify(2, 1));
    }

    @Test
    public void twoIncomingMatchesMustNotAutomaticallyReplace() {
        assertEquals(TitleMatching.Kind.AMBIGUOUS, TitleMatching.classify(1, 2));
        assertEquals(TitleMatching.Kind.AMBIGUOUS, TitleMatching.classify(0, 2));
    }

    @Test
    public void comparisonsIgnoreCaseAndOuterWhitespace() {
        assertEquals(TitleMatching.normalize("  House Door "),
                TitleMatching.normalize("house door"));
    }

    @Test
    public void invalidCountsFail() {
        assertThrows(IllegalArgumentException.class,
                () -> TitleMatching.classify(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TitleMatching.classify(0, 0));
    }
}
