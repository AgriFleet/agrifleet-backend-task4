package com.agrifleet.decision.algorithm;

import java.util.ArrayList;
import java.util.List;

public class TopsisEngine {

    public static class Alternative {
        public Long vehicleId;
        public double[] criteriaValues;
        public double sPlus;
        public double sMinus;
        public double closenessScore;
        public int rank;

        public Alternative(Long vehicleId, double[] criteriaValues) {
            this.vehicleId = vehicleId;
            this.criteriaValues = criteriaValues;
        }
    }

    public static List<Alternative> calculate(List<Alternative> alternatives, double[] weights, boolean[] isBeneficial) {
        int m = alternatives.size();
        int n = weights.length;

        // 1. Vector Normalization
        double[] denominators = new double[n];
        for (int j = 0; j < n; j++) {
            double sumSq = 0;
            for (int i = 0; i < m; i++) sumSq += Math.pow(alternatives.get(i).criteriaValues[j], 2);
            denominators[j] = Math.sqrt(sumSq);
        }

        // 2. Weighted Normalized Matrix
        double[][] vMatrix = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                vMatrix[i][j] = (alternatives.get(i).criteriaValues[j] / denominators[j]) * weights[j];
            }
        }

        // 3. Ideal Best (A+) and Ideal Worst (A-)
        double[] aPlus = new double[n];
        double[] aMinus = new double[n];
        for (int j = 0; j < n; j++) {
            aPlus[j] = isBeneficial[j] ? -Double.MAX_VALUE : Double.MAX_VALUE;
            aMinus[j] = isBeneficial[j] ? Double.MAX_VALUE : -Double.MAX_VALUE;
            for (int i = 0; i < m; i++) {
                if (isBeneficial[j]) {
                    if (vMatrix[i][j] > aPlus[j]) aPlus[j] = vMatrix[i][j];
                    if (vMatrix[i][j] < aMinus[j]) aMinus[j] = vMatrix[i][j];
                } else {
                    if (vMatrix[i][j] < aPlus[j]) aPlus[j] = vMatrix[i][j];
                    if (vMatrix[i][j] > aMinus[j]) aMinus[j] = vMatrix[i][j];
                }
            }
        }

        // 4. Calculate Separation Measures and 5. Performance Score
        for (int i = 0; i < m; i++) {
            double distPlus = 0;
            double distMinus = 0;
            for (int j = 0; j < n; j++) {
                distPlus += Math.pow(vMatrix[i][j] - aPlus[j], 2);
                distMinus += Math.pow(vMatrix[i][j] - aMinus[j], 2);
            }
            alternatives.get(i).sPlus = Math.sqrt(distPlus);
            alternatives.get(i).sMinus = Math.sqrt(distMinus);
            alternatives.get(i).closenessScore = alternatives.get(i).sMinus / (alternatives.get(i).sPlus + alternatives.get(i).sMinus);
        }

        // 6. Sort Alternatives Descending
        alternatives.sort((a, b) -> Double.compare(b.closenessScore, a.closenessScore));
        for (int i = 0; i < m; i++) {
            alternatives.get(i).rank = i + 1;
        }

        return alternatives;
    }
}