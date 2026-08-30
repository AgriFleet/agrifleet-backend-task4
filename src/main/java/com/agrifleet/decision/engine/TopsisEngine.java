package com.agrifleet.decision.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TOPSIS (Technique for Order Preference by Similarity to Ideal Solution).
 *
 * Pure algorithm implementation - no Spring / DB dependencies - so it can be
 * unit tested in isolation and reused directly for the Chapter 7/8 complexity
 * and benchmarking sections of the individual report.
 *
 * Steps (matches the coursework spec pseudocode):
 *   1. Vector-normalize the decision matrix.
 *   2. Apply criteria weights.
 *   3. Determine the ideal best (A+) and ideal worst (A-) solutions.
 *   4. Compute Euclidean separation of each alternative from A+ and A-.
 *   5. Compute relative closeness C(i) = S-/(S+ + S-).
 *   6. Rank alternatives descending by C(i).
 *
 * Complexity:
 *   Time  : O(M * N)  where M = number of alternatives (vehicles), N = number of criteria.
 *           Every step is a single pass (or two, for normalization) over the M x N matrix,
 *           plus an O(M log M) sort at the end - dominated by O(M*N) for realistic N (<10).
 *   Space : O(M * N) for the normalized/weighted matrices, O(N) for A+/A-, O(M) for results.
 */
public final class TopsisEngine {

    private TopsisEngine() {
    }

    /**
     * @param matrix        M x N raw decision matrix (rows = alternatives, cols = criteria)
     * @param weights       length-N array of criteria weights (need not be pre-normalized to sum 1;
     *                      caller should normalize before calling for a meaningful A+/A- comparison)
     * @param isBeneficial  length-N array; true = "higher is better" (e.g. horsepower, rating),
     *                      false = "lower is better" (e.g. cost, distance)
     */
    public static TopsisResult rank(double[][] matrix, double[] weights, boolean[] isBeneficial) {
        int m = matrix.length;
        if (m == 0) {
            throw new IllegalArgumentException("Decision matrix must contain at least one alternative");
        }
        int n = matrix[0].length;
        if (weights.length != n || isBeneficial.length != n) {
            throw new IllegalArgumentException("weights and isBeneficial must have length == number of criteria");
        }

        // --- Step 1: vector normalization (denominator = sqrt(sum of squares) per column) ---
        double[] columnNorms = new double[n];
        for (int j = 0; j < n; j++) {
            double sumSquares = 0.0;
            for (int i = 0; i < m; i++) {
                sumSquares += matrix[i][j] * matrix[i][j];
            }
            columnNorms[j] = Math.sqrt(sumSquares);
        }

        double[][] normalized = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                normalized[i][j] = columnNorms[j] == 0.0 ? 0.0 : matrix[i][j] / columnNorms[j];
            }
        }

        // --- Step 2: weighted normalized matrix ---
        double[][] weighted = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                weighted[i][j] = normalized[i][j] * weights[j];
            }
        }

        // --- Step 3: ideal best (A+) and ideal worst (A-) ---
        double[] aPlus = new double[n];
        double[] aMinus = new double[n];
        for (int j = 0; j < n; j++) {
            double colMax = Double.NEGATIVE_INFINITY;
            double colMin = Double.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                colMax = Math.max(colMax, weighted[i][j]);
                colMin = Math.min(colMin, weighted[i][j]);
            }
            if (isBeneficial[j]) {
                aPlus[j] = colMax;
                aMinus[j] = colMin;
            } else {
                aPlus[j] = colMin;
                aMinus[j] = colMax;
            }
        }

        // --- Step 4: separation measures (Euclidean distance) ---
        double[] sPlus = new double[m];
        double[] sMinus = new double[m];
        for (int i = 0; i < m; i++) {
            double sp = 0.0;
            double sm = 0.0;
            for (int j = 0; j < n; j++) {
                sp += Math.pow(weighted[i][j] - aPlus[j], 2);
                sm += Math.pow(weighted[i][j] - aMinus[j], 2);
            }
            sPlus[i] = Math.sqrt(sp);
            sMinus[i] = Math.sqrt(sm);
        }

        // --- Step 5: relative closeness C(i) ---
        double[] closeness = new double[m];
        for (int i = 0; i < m; i++) {
            double denom = sPlus[i] + sMinus[i];
            closeness[i] = denom == 0.0 ? 0.0 : sMinus[i] / denom;
        }

        // --- Step 6: rank descending by closeness ---
        List<AlternativeScore> scored = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            scored.add(new AlternativeScore(i, sPlus[i], sMinus[i], closeness[i]));
        }
        scored.sort(Comparator.comparingDouble(AlternativeScore::closeness).reversed());
        for (int rank = 0; rank < scored.size(); rank++) {
            scored.get(rank).setRank(rank + 1);
        }
        // restore original alternative order but keep rank field populated
        scored.sort(Comparator.comparingInt(AlternativeScore::index));

        return new TopsisResult(aPlus, aMinus, scored);
    }

    /** Result row for one alternative (one candidate vehicle). */
    public static final class AlternativeScore {
        private final int index;
        private final double sPlus;
        private final double sMinus;
        private final double closeness;
        private int rank;

        AlternativeScore(int index, double sPlus, double sMinus, double closeness) {
            this.index = index;
            this.sPlus = sPlus;
            this.sMinus = sMinus;
            this.closeness = closeness;
        }

        public int index() { return index; }
        public double sPlus() { return sPlus; }
        public double sMinus() { return sMinus; }
        public double closeness() { return closeness; }
        public int rank() { return rank; }
        void setRank(int rank) { this.rank = rank; }
    }

    /** Full TOPSIS run output: ideal vectors + per-alternative scores/ranks. */
    public static final class TopsisResult {
        private final double[] idealBest;
        private final double[] idealWorst;
        private final List<AlternativeScore> scores;

        TopsisResult(double[] idealBest, double[] idealWorst, List<AlternativeScore> scores) {
            this.idealBest = idealBest;
            this.idealWorst = idealWorst;
            this.scores = scores;
        }

        public double[] idealBest() { return idealBest; }
        public double[] idealWorst() { return idealWorst; }
        public List<AlternativeScore> scores() { return scores; }
    }
}
