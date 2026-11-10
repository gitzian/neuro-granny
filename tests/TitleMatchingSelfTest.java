import org.gitzian.neurogranny.backup.TitleMatching;

/** Pure-Java import identity rules, independent of the Android framework. */
public final class TitleMatchingSelfTest {

    private static int passed;

    private TitleMatchingSelfTest() {
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        passed++;
    }

    public static void main(String[] args) {
        check(TitleMatching.classify(0, 1) == TitleMatching.Kind.NEW,
                "new title");
        check(TitleMatching.classify(1, 1) == TitleMatching.Kind.UNIQUE,
                "unique title on both sides");
        check(TitleMatching.classify(2, 1) == TitleMatching.Kind.AMBIGUOUS,
                "two local records are ambiguous");
        check(TitleMatching.classify(1, 2) == TitleMatching.Kind.AMBIGUOUS,
                "two incoming records are ambiguous");
        check(TitleMatching.classify(0, 2) == TitleMatching.Kind.AMBIGUOUS,
                "incoming duplicates without local entry are ambiguous");
        check(TitleMatching.normalize("  My Secret ").equals(
                TitleMatching.normalize("my secret")),
                "title matching ignores case and surrounding whitespace");

        System.out.println("TitleMatchingSelfTest: " + passed + " checks passed");
    }
}
