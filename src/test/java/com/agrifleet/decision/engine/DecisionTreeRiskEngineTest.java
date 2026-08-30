package com.agrifleet.decision.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionTreeRiskEngineTest {

    @Test
    void trainsAndPredictsObviousCases() {
        DecisionTreeRiskEngine tree = new DecisionTreeRiskEngine(5, 6);
        DecisionTreeRiskEngine.TrainingSet data = DecisionTreeRiskEngine.generateSyntheticTrainingData(800, 42L);
        tree.fit(data.features, data.labels);

        // Low acreage, no rain, no breakdown history -> should predict LOW_RISK
        DecisionTreeRiskEngine.Prediction low = tree.predict(new double[]{10, 0.05, 0});
        assertEquals(DecisionTreeRiskEngine.LOW_RISK, low.riskTier);

        // Large acreage, near-certain rain, repeated breakdowns -> should predict CRITICAL_DELAY
        DecisionTreeRiskEngine.Prediction critical = tree.predict(new double[]{90, 0.95, 4});
        assertEquals(DecisionTreeRiskEngine.CRITICAL_DELAY, critical.riskTier);
    }

    @Test
    void confidenceIsBoundedZeroToOne() {
        DecisionTreeRiskEngine tree = new DecisionTreeRiskEngine();
        DecisionTreeRiskEngine.TrainingSet data = DecisionTreeRiskEngine.generateSyntheticTrainingData(400, 1L);
        tree.fit(data.features, data.labels);

        DecisionTreeRiskEngine.Prediction p = tree.predict(new double[]{45, 0.5, 1});
        assertTrue(p.confidence >= 0.0 && p.confidence <= 1.0);
    }
}
