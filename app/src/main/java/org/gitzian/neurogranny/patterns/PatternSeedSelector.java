package org.gitzian.neurogranny.patterns;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects a fresh visual cue that differs from currently assigned cues. */
public final class PatternSeedSelector {
    private static final int CANDIDATES_PER_SELECTION = 96;

    private PatternSeedSelector() {
    }

    /**
     * Generates independent random candidates and chooses the candidate whose
     * closest existing cue is furthest away in structural feature space.
     *
     * <p>The selector does not modify or regenerate existing cues. It only
     * chooses the most distinct candidate among the fresh random options.
     */
    public static long choose(SecureRandom secureRandom, List<Long> assignedSeeds) {
        List<PatternDesign> existing = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        for (Long assignedSeed : assignedSeeds) {
            if (assignedSeed != null && occupied.add(assignedSeed)) {
                existing.add(PatternDesign.fromSeed(assignedSeed));
            }
        }

        long bestSeed = 0L;
        double bestMinimumDistance = Double.NEGATIVE_INFINITY;
        boolean found = false;

        for (int attempt = 0; attempt < CANDIDATES_PER_SELECTION; attempt++) {
            long candidateSeed = secureRandom.nextLong();
            if (occupied.contains(candidateSeed)) {
                continue;
            }

            PatternDesign candidate = PatternDesign.fromSeed(candidateSeed);
            double minimumDistance = Double.POSITIVE_INFINITY;

            for (PatternDesign prior : existing) {
                minimumDistance = Math.min(minimumDistance, candidate.distanceFrom(prior));
            }

            if (existing.isEmpty()) {
                minimumDistance = 1.0;
            }

            if (!found || minimumDistance > bestMinimumDistance) {
                bestSeed = candidateSeed;
                bestMinimumDistance = minimumDistance;
                found = true;
            }
        }

        if (!found) {
            do {
                bestSeed = secureRandom.nextLong();
            } while (occupied.contains(bestSeed));
        }

        return bestSeed;
    }
}
