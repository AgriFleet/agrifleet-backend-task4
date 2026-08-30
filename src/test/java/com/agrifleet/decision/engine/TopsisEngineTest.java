package com.agrifleet.decision.engine;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TopsisEngineTest {

    /**
     * Uses the 4 candidates from the coursework's seed data (vehicles 1, 2, 3, 8
     * from the SQL script) but asserts against a HAND-VERIFIED TOPSIS result,
     * not the ranked_fleet_candidates seed rows themselves.
     *
     * IMPORTANT: the seed data's own closeness scores (C=0.676 for vehicle 1,
     * C=0.551 for vehicle 8, C=0.342 for vehicle 3 - implying order 2,1,8,3)
     * do NOT match what the TOPSIS formula actually produces for these inputs.
     * Manually recomputing normalization -> weighting -> A+/A- -> separation
     * gives C(2)=0.713, C(1)=0.511, C(3)=0.406, C(8)=0.367 - i.e. vehicle 3
     * (cheapest at $95/hr, cost carries the highest weight at 0.35) actually
     * outranks vehicle 8 despite worse horsepower/rating. The seed script's
     * numbers are illustrative placeholders, not a verified ground truth -
     * flag this in Chapter 8 if you cite the seed data anywhere, and use the
     * numbers below (or your own hand calculation) as the authoritative check.
     */
    @Test
    void ranksSeedDataInExpectedOrder() {
        // columns: [hourly_rate, distance_km, horsepower, rating]
        double[][] matrix = {
                {145.00, 5.85, 380, 4.92}, // vehicle 1
                {115.00, 4.20, 290, 4.75}, // vehicle 2
                {95.00,  9.10, 210, 4.60}, // vehicle 3
                {135.00, 7.80, 340, 4.85}, // vehicle 8
        };
        double[] weights = {0.35, 0.25, 0.20, 0.20};
        boolean[] isBeneficial = {false, false, true, true};

        TopsisEngine.TopsisResult result = TopsisEngine.rank(matrix, weights, isBeneficial);

        int rankOfVehicle1 = result.scores().get(0).rank();
        int rankOfVehicle2 = result.scores().get(1).rank();
        int rankOfVehicle3 = result.scores().get(2).rank();
        int rankOfVehicle8 = result.scores().get(3).rank();

        // Correct order (hand-verified): 2 > 1 > 3 > 8
        assertTrue(rankOfVehicle2 < rankOfVehicle1, "cheaper/closer vehicle 2 should outrank vehicle 1");
        assertTrue(rankOfVehicle1 < rankOfVehicle3, "vehicle 1 should outrank vehicle 3");
        assertTrue(rankOfVehicle3 < rankOfVehicle8, "vehicle 3 should outrank vehicle 8 (cost-dominated result)");
        assertEquals(1, rankOfVehicle2);
        assertEquals(4, rankOfVehicle8);

        // Sanity-check closeness values against the hand calculation (tolerance 0.01)
        double c1 = result.scores().get(0).closeness();
        double c2 = result.scores().get(1).closeness();
        double c3 = result.scores().get(2).closeness();
        double c8 = result.scores().get(3).closeness();
        assertEquals(0.511, c1, 0.01);
        assertEquals(0.713, c2, 0.01);
        assertEquals(0.406, c3, 0.01);
        assertEquals(0.367, c8, 0.01);
    }

    @Test
    void closenessScoresAreWithinZeroToOne() {
        double[][] matrix = {{10, 2}, {20, 5}, {15, 1}};
        double[] weights = {0.5, 0.5};
        boolean[] isBeneficial = {false, true};

        TopsisEngine.TopsisResult result = TopsisEngine.rank(matrix, weights, isBeneficial);
        for (TopsisEngine.AlternativeScore s : result.scores()) {
            assertTrue(s.closeness() >= 0.0 && s.closeness() <= 1.0);
        }
    }

    @Test
    void singleAlternativeIsRankOne() {
        double[][] matrix = {{50, 3, 200, 4.5}};
        double[] weights = {0.25, 0.25, 0.25, 0.25};
        boolean[] isBeneficial = {false, false, true, true};

        TopsisEngine.TopsisResult result = TopsisEngine.rank(matrix, weights, isBeneficial);
        assertEquals(1, result.scores().get(0).rank());
    }

    /**
     * Not a strict assertion test - prints timing for M = 20..2000 alternatives
     * as suggested by the coursework's Chapter 8 experimental table
     * ("Candidate Machines M in [20, 2000], Linear O(M * N)").
     * Run manually and paste the output into your report's benchmark table/chart.
     */
    @Test
    void benchmarkAcrossInputSizes() {
        Random rnd = new Random(7);
        int[] sizes = {20, 50, 100, 500, 1000, 2000};
        double[] weights = {0.35, 0.25, 0.20, 0.20};
        boolean[] isBeneficial = {false, false, true, true};

        for (int m : sizes) {
            double[][] matrix = new double[m][4];
            for (int i = 0; i < m; i++) {
                matrix[i][0] = 50 + rnd.nextDouble() * 150;   // cost
                matrix[i][1] = rnd.nextDouble() * 20;         // distance
                matrix[i][2] = 80 + rnd.nextDouble() * 350;   // hp
                matrix[i][3] = 3 + rnd.nextDouble() * 2;      // rating
            }
            long start = System.nanoTime();
            TopsisEngine.rank(matrix, weights, isBeneficial);
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            System.out.printf("M=%-5d  time=%.3f ms%n", m, elapsedMs);
        }
    }
}
