package org.gitzian.neurogranny.patterns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PatternDesignTest {
    private static final int SAMPLE_SEED_COUNT = 1_000;
    private static final int ASSIGNED_SEED_COUNT = 40;

    @Test
    public void sameSeedProducesSameDesign() {
        for (long seed = 0; seed < SAMPLE_SEED_COUNT; seed++) {
            PatternDesign first = PatternDesign.fromSeed(seed);
            PatternDesign second = PatternDesign.fromSeed(seed);

            assertEquals(seed, first.seed);
            assertEquals(first.archetype, second.archetype);
            assertEquals(first.backgroundMode, second.backgroundMode);
            assertEquals(first.paletteIndex, second.paletteIndex);
            assertEquals(first.backgroundCorner, second.backgroundCorner);
            assertEquals(first.anchorCorner, second.anchorCorner);
            assertEquals(first.accentRadius, second.accentRadius);
            assertEquals(first.rotationDegrees, second.rotationDegrees);
            assertEquals(first.mirrored, second.mirrored);
            assertEquals(first.offsetX, second.offsetX);
            assertEquals(first.offsetY, second.offsetY);
            assertEquals(first.glyphScale, second.glyphScale, 0.0f);
            assertEquals(0.0, first.distanceFrom(second), 0.0);
        }
    }

    @Test
    public void allArchetypesAndBackgroundModesAreReachable() {
        Set<PatternDesign.Archetype> archetypes = EnumSet.noneOf(PatternDesign.Archetype.class);
        Set<PatternDesign.BackgroundMode> backgrounds = EnumSet.noneOf(PatternDesign.BackgroundMode.class);

        for (long seed = 0; seed < SAMPLE_SEED_COUNT; seed++) {
            PatternDesign design = PatternDesign.fromSeed(seed);
            archetypes.add(design.archetype);
            backgrounds.add(design.backgroundMode);
            assertNotEquals(design.backgroundCorner, design.anchorCorner);
        }

        assertEquals(PatternDesign.Archetype.values().length, archetypes.size());
        assertEquals(PatternDesign.BackgroundMode.values().length, backgrounds.size());
    }

    @Test
    public void selectionDoesNotReuseAssignedSeeds() {
        SecureRandom random = new SecureRandom();
        List<Long> assigned = new ArrayList<>();
        Set<Long> distinct = new HashSet<>();

        for (int index = 0; index < ASSIGNED_SEED_COUNT; index++) {
            long selected = PatternSeedSelector.choose(random, assigned);
            assertFalse(assigned.contains(selected));
            assertTrue(distinct.add(selected));
            assigned.add(selected);
        }
    }
}
