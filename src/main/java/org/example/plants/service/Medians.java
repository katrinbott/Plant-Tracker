package org.example.plants.service;

import java.util.List;

/**
 * Shared between a plant's median gap and a room's median of those, so the two cannot
 * drift apart if the rule ever changes.
 */
final class Medians {
    private Medians() {}

    /** The list must not be empty: with nothing to measure there is no median, only null. */
    static double of(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }
}
