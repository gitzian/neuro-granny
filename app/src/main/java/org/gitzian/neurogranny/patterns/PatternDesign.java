package org.gitzian.neurogranny.patterns;

import java.util.Random;

/**
 * Immutable description of a visual cue, derived entirely from its random seed.
 *
 * <p>The current design system favors distinctive silhouettes over dense visual
 * texture. A pattern is built from a dominant glyph archetype, a simple corner
 * anchor and a low-noise background shape. That combination produces cues that
 * differ more strongly at a glance than the previous texture-heavy generator.
 */
public final class PatternDesign {
    public static final int FORMAT_VERSION = 2;

    public enum Archetype {
        FORKED_RUNE,
        OPEN_RINGS,
        SHARD,
        TOTEM,
        DOUBLE_CHEVRON,
        ORBIT,
        CURVED_WAVE,
        LADDER,
        HOOK,
        COMPASS
    }

    public enum BackgroundMode {
        CIRCLE,
        TRIANGLE,
        ROUNDED_BLOCK,
        ARC,
        SHARD
    }

    private static final long DESIGN_SEED_SCRAMBLE = 0x4e6575726f477261L;
    private static final int PALETTE_COUNT = 10;
    private static final int CORNER_COUNT = 4;

    public final long seed;
    public final Archetype archetype;
    public final BackgroundMode backgroundMode;
    public final int paletteIndex;
    public final int backgroundCorner;
    public final int anchorCorner;
    public final int accentRadius;
    public final int rotationDegrees;
    public final boolean mirrored;
    public final int offsetX;
    public final int offsetY;
    public final float glyphScale;

    private PatternDesign(long seed, Random random) {
        this.seed = seed;
        archetype = Archetype.values()[random.nextInt(Archetype.values().length)];
        backgroundMode = BackgroundMode.values()[random.nextInt(BackgroundMode.values().length)];
        paletteIndex = random.nextInt(PALETTE_COUNT);
        backgroundCorner = random.nextInt(CORNER_COUNT);
        anchorCorner = chooseDifferentCorner(random, backgroundCorner);
        accentRadius = random.nextBoolean() ? 20 : (random.nextBoolean() ? 28 : 38);
        rotationDegrees = chooseRotation(random);
        mirrored = random.nextBoolean();
        offsetX = random.nextInt(81) - 40;
        offsetY = random.nextInt(81) - 40;
        glyphScale = 0.92f + random.nextFloat() * 0.18f;
    }

    public static PatternDesign fromSeed(long seed) {
        return new PatternDesign(seed, new Random(seed ^ DESIGN_SEED_SCRAMBLE));
    }

    /**
     * Structural distance, tuned for human-perceived differences.
     *
     * <p>Glyph silhouette matters most, then palette and corner anchors. The
     * remaining values help distinguish designs within the same archetype.
     */
    public double distanceFrom(PatternDesign other) {
        if (seed == other.seed) {
            return 0.0;
        }

        double score = 0.0;
        if (archetype != other.archetype) {
            score += 8.0;
        }
        if (paletteIndex != other.paletteIndex) {
            score += 3.5;
        }
        if (backgroundMode != other.backgroundMode) {
            score += 2.5;
        }
        if (backgroundCorner != other.backgroundCorner) {
            score += 1.5;
        }
        if (anchorCorner != other.anchorCorner) {
            score += 1.5;
        }
        score += Math.abs(accentRadius - other.accentRadius) / 10.0;
        score += Math.abs(rotationDegrees - other.rotationDegrees) / 12.0;
        if (mirrored != other.mirrored) {
            score += 0.8;
        }
        score += Math.abs(offsetX - other.offsetX) / 35.0;
        score += Math.abs(offsetY - other.offsetY) / 35.0;
        score += Math.abs(glyphScale - other.glyphScale) * 2.0;
        return score;
    }

    private static int chooseDifferentCorner(Random random, int disallowedCorner) {
        int corner = random.nextInt(CORNER_COUNT - 1);
        if (corner >= disallowedCorner) {
            corner++;
        }
        return corner;
    }

    private static int chooseRotation(Random random) {
        int degrees;
        do {
            degrees = random.nextInt(43) - 21;
        } while (Math.abs(degrees) < 5);
        return degrees;
    }
}
