package com.agrifleet.decision.service;

import com.agrifleet.decision.algorithm.TopsisEngine;
import com.agrifleet.decision.entity.*;
import com.agrifleet.decision.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class DecisionSupportService {

    private final TopsisDecisionRunRepository topsisRepository;
    private final RankedFleetCandidateRepository rankedRepository;
    private final HarvestDelayPredictionRepository delayRepository;

    public DecisionSupportService(TopsisDecisionRunRepository topsisRepository,
                                  RankedFleetCandidateRepository rankedRepository,
                                  HarvestDelayPredictionRepository delayRepository) {
        this.topsisRepository = topsisRepository;
        this.rankedRepository = rankedRepository;
        this.delayRepository = delayRepository;
    }

    public List<TopsisDecisionRunEntity> getAllTopsisRuns() {
        return topsisRepository.findAll();
    }

    public List<RankedFleetCandidateEntity> getRankedCandidates(Long runId) {
        return rankedRepository.findByDecisionRunIdOrderByFinalRankAsc(runId);
    }

    public List<HarvestDelayPredictionEntity> getAllDelayPredictions() {
        return delayRepository.findAll();
    }

    public TopsisDecisionRunEntity runTopsisRanking(Long bookingId, Map<String, Double> weightsMap) {
        // 1. Create Decision Run Record
        TopsisDecisionRunEntity run = new TopsisDecisionRunEntity();
        run.setBookingId(bookingId);
        run.setFarmerId(1L);
        run.setCriteriaWeights(weightsMap.toString());
        run.setIdealBestVectorAPlus("{}"); // Simplified for brevity
        run.setIdealWorstVectorAMinus("{}");
        run = topsisRepository.save(run);

        // 2. Fetch/Mock Candidate Vehicles (Cost, Distance, HP, Rating)
        List<TopsisEngine.Alternative> alternatives = new ArrayList<>();
        alternatives.add(new TopsisEngine.Alternative(1L, new double[]{145.0, 5.85, 380, 4.92}));
        alternatives.add(new TopsisEngine.Alternative(2L, new double[]{115.0, 4.20, 290, 4.75}));
        alternatives.add(new TopsisEngine.Alternative(3L, new double[]{95.0, 9.10, 210, 4.60}));

        // Weights & Beneficial impact: Cost(-), Distance(-), HP(+), Rating(+)
        double[] weights = {0.35, 0.25, 0.20, 0.20};
        boolean[] isBeneficial = {false, false, true, true};

        // 3. Evaluate via Topsis Engine
        List<TopsisEngine.Alternative> ranked = TopsisEngine.calculate(alternatives, weights, isBeneficial);

        // 4. Save Rankings to DB
        for (TopsisEngine.Alternative alt : ranked) {
            RankedFleetCandidateEntity rc = new RankedFleetCandidateEntity();
            rc.setDecisionRunId(run.getDecisionRunId());
            rc.setVehicleId(alt.vehicleId);
            rc.setHourlyRate(alt.criteriaValues[0]);
            rc.setDistanceKm(alt.criteriaValues[1]);
            rc.setHorsepower((int) alt.criteriaValues[2]);
            rc.setRatingScore(alt.criteriaValues[3]);
            rc.setSeparationSPlus(alt.sPlus);
            rc.setSeparationSMinus(alt.sMinus);
            rc.setRelativeClosenessC(alt.closenessScore);
            rc.setFinalRank(alt.rank);
            rankedRepository.save(rc);
        }

        return run;
    }

    public HarvestDelayPredictionEntity predictHarvestDelay(Long bookingId) {
        // Simulating Information Gain / Decision Tree rule evaluation
        Random rand = new Random();
        double rainProb = rand.nextDouble();
        int breakdownHistory = rand.nextInt(3);

        String tier = "LOW_RISK";
        if (rainProb > 0.7 && breakdownHistory > 1) {
            tier = "CRITICAL_DELAY";
        } else if (rainProb > 0.4 || breakdownHistory > 0) {
            tier = "MODERATE_RISK";
        }

        HarvestDelayPredictionEntity pred = new HarvestDelayPredictionEntity();
        pred.setBookingId(bookingId);
        pred.setFieldAcres(20.0 + (rand.nextDouble() * 30.0));
        pred.setRainProbability(rainProb);
        pred.setVehicleBreakdownHistory(breakdownHistory);
        pred.setPredictedRiskTier(tier);
        pred.setConfidenceScore(0.85 + (rand.nextDouble() * 0.10));

        return delayRepository.save(pred);
    }
}