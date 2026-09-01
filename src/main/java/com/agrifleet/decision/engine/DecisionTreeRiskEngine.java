package com.agrifleet.decision.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
  A compact, from-scratch CART-style decision tree classifier used for the Harvest Delay Risk Predictor (Task 4 secondary feature).

  This is the coursework's second/comparison algorithm alongside TOPSIS (the spec's own comparison table lists "TOPSIS vs k-NN/Decision Tree" -
  we build the decision tree branch for real so Chapter 3's "investigate
  >= 3 candidate algorithms" has genuine implemented evidence, not just
  literature discussion).

  Features used per sample:
    x0 = field_acres
    x1 = rain_probability            (0.0 - 1.0)
    x2 = vehicle_breakdown_history   (integer count, cast to double)
  Label: one of LOW_RISK, MODERATE_RISK, CRITICAL_DELAY

  Split criterion: Gini impurity, evaluated at the midpoint between every
  pair of consecutive sorted values of a feature (standard CART numeric
  split search).

  Complexity (training):
    Time  : O(depth * N * F * log N) - at each of up to `depth` levels we
            consider F features, and for each feature we sort O(N) values
            (O(N log N)) to scan candidate thresholds.
    Space : O(N) per level for the working subsets, O(2^depth) for the tree
            nodes in the worst case (bounded by maxDepth here).
  Complexity (prediction): O(depth) - a single root-to-leaf walk.
 */
public class DecisionTreeRiskEngine {

    public static final String LOW_RISK = "LOW_RISK";
    public static final String MODERATE_RISK = "MODERATE_RISK";
    public static final String CRITICAL_DELAY = "CRITICAL_DELAY";

    private final int maxDepth;
    private final int minSamplesSplit;
    private Node root;

    public DecisionTreeRiskEngine(int maxDepth, int minSamplesSplit) {
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
    }

    public DecisionTreeRiskEngine() {
        this(5, 6);
    }

    // ------------------------------------------------------------------
    // Training
    // ------------------------------------------------------------------

    public void fit(List<double[]> features, List<String> labels) {
        if (features.size() != labels.size() || features.isEmpty()) {
            throw new IllegalArgumentException("features/labels must be non-empty and equal length");
        }
        List<Integer> allIdx = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) allIdx.add(i);
        this.root = buildNode(features, labels, allIdx, 0);
    }

    private Node buildNode(List<double[]> X, List<String> y, List<Integer> idx, int depth) {
        Map<String, Integer> counts = classCounts(y, idx);

        // Stopping conditions -> leaf
        if (depth >= maxDepth || idx.size() < minSamplesSplit || counts.size() == 1) {
            return Node.leaf(majorityClass(counts), classProbabilities(counts, idx.size()));
        }

        BestSplit best = findBestSplit(X, y, idx);
        if (best == null) {
            return Node.leaf(majorityClass(counts), classProbabilities(counts, idx.size()));
        }

        Node node = new Node();
        node.featureIndex = best.featureIndex;
        node.threshold = best.threshold;
        node.left = buildNode(X, y, best.leftIdx, depth + 1);
        node.right = buildNode(X, y, best.rightIdx, depth + 1);
        return node;
    }

    private BestSplit findBestSplit(List<double[]> X, List<String> y, List<Integer> idx) {
        int numFeatures = X.get(idx.get(0)).length;
        double parentGini = gini(classCounts(y, idx), idx.size());

        BestSplit best = null;
        double bestGain = 0.0;

        for (int f = 0; f < numFeatures; f++) {
            List<Double> sortedVals = new ArrayList<>();
            for (int i : idx) sortedVals.add(X.get(i)[f]);
            sortedVals.sort(Double::compareTo);

            for (int k = 0; k < sortedVals.size() - 1; k++) {
                double a = sortedVals.get(k);
                double b = sortedVals.get(k + 1);
                if (a == b) continue;
                double threshold = (a + b) / 2.0;

                List<Integer> leftIdx = new ArrayList<>();
                List<Integer> rightIdx = new ArrayList<>();
                for (int i : idx) {
                    if (X.get(i)[f] <= threshold) leftIdx.add(i);
                    else rightIdx.add(i);
                }
                if (leftIdx.isEmpty() || rightIdx.isEmpty()) continue;

                double giniLeft = gini(classCounts(y, leftIdx), leftIdx.size());
                double giniRight = gini(classCounts(y, rightIdx), rightIdx.size());
                double weighted = (leftIdx.size() * giniLeft + rightIdx.size() * giniRight) / idx.size();
                double gain = parentGini - weighted;

                if (gain > bestGain) {
                    bestGain = gain;
                    best = new BestSplit(f, threshold, leftIdx, rightIdx);
                }
            }
        }
        return best;
    }

    private double gini(Map<String, Integer> counts, int total) {
        if (total == 0) return 0.0;
        double impurity = 1.0;
        for (int c : counts.values()) {
            double p = (double) c / total;
            impurity -= p * p;
        }
        return impurity;
    }

    private Map<String, Integer> classCounts(List<String> y, List<Integer> idx) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i : idx) counts.merge(y.get(i), 1, Integer::sum);
        return counts;
    }

    private String majorityClass(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(LOW_RISK);
    }

    private Map<String, Double> classProbabilities(Map<String, Integer> counts, int total) {
        Map<String, Double> probs = new HashMap<>();
        counts.forEach((k, v) -> probs.put(k, total == 0 ? 0.0 : (double) v / total));
        return probs;
    }


    // Prediction


    public Prediction predict(double[] features) {
        if (root == null) {
            throw new IllegalStateException("Tree has not been trained - call fit() first");
        }
        Node node = root;
        while (!node.isLeaf()) {
            node = features[node.featureIndex] <= node.threshold ? node.left : node.right;
        }
        double confidence = node.classProbabilities.getOrDefault(node.predictedClass, 0.0);
        return new Prediction(node.predictedClass, confidence);
    }

    /*
      Generates a synthetic-but-domain-informed training set. Real deployments
      would train on accumulated `harvest_delay_predictions` history once
      enough rows exist; for the coursework we bootstrap with rule-derived
      synthetic samples plus label noise so the tree has to genuinely learn
      boundaries rather than memorize a lookup table. Document this choice
      explicitly in Chapter 3/8 of the individual report.
     */
    public static TrainingSet generateSyntheticTrainingData(int numSamples, long seed) {
        Random rnd = new Random(seed);
        List<double[]> X = new ArrayList<>(numSamples);
        List<String> y = new ArrayList<>(numSamples);

        for (int i = 0; i < numSamples; i++) {
            double acres = 5 + rnd.nextDouble() * 95;          // 5 - 100 acres
            double rain = rnd.nextDouble();                     // 0 - 1
            int breakdowns = rnd.nextInt(5);                    // 0 - 4 prior breakdowns

            double riskScore = 0.55 * rain
                    + 0.25 * Math.min(1.0, breakdowns / 3.0)
                    + 0.20 * Math.min(1.0, acres / 80.0);
            riskScore += (rnd.nextDouble() - 0.5) * 0.12; // label noise

            String label;
            if (riskScore < 0.35) label = LOW_RISK;
            else if (riskScore < 0.65) label = MODERATE_RISK;
            else label = CRITICAL_DELAY;

            X.add(new double[]{acres, rain, breakdowns});
            y.add(label);
        }
        return new TrainingSet(X, y);
    }


    // Small value types


    private static final class Node {
        int featureIndex;
        double threshold;
        Node left;
        Node right;
        String predictedClass;
        Map<String, Double> classProbabilities;

        static Node leaf(String predictedClass, Map<String, Double> probs) {
            Node n = new Node();
            n.predictedClass = predictedClass;
            n.classProbabilities = probs;
            return n;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    private static final class BestSplit {
        final int featureIndex;
        final double threshold;
        final List<Integer> leftIdx;
        final List<Integer> rightIdx;

        BestSplit(int featureIndex, double threshold, List<Integer> leftIdx, List<Integer> rightIdx) {
            this.featureIndex = featureIndex;
            this.threshold = threshold;
            this.leftIdx = leftIdx;
            this.rightIdx = rightIdx;
        }
    }

    public static final class TrainingSet {
        public final List<double[]> features;
        public final List<String> labels;

        TrainingSet(List<double[]> features, List<String> labels) {
            this.features = features;
            this.labels = labels;
        }
    }

    public static final class Prediction {
        public final String riskTier;
        public final double confidence;

        public Prediction(String riskTier, double confidence) {
            this.riskTier = riskTier;
            this.confidence = confidence;
        }
    }
}
