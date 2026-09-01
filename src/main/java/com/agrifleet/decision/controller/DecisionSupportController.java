package com.agrifleet.decision.controller;

import com.agrifleet.decision.entity.HarvestDelayPredictionEntity;
import com.agrifleet.decision.entity.RankedFleetCandidateEntity;
import com.agrifleet.decision.entity.TopsisDecisionRunEntity;
import com.agrifleet.decision.service.DecisionSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class DecisionSupportController {

    private final DecisionSupportService decisionSupportService;

    public DecisionSupportController(DecisionSupportService decisionSupportService) {
        this.decisionSupportService = decisionSupportService;
    }

    @GetMapping("/api/v1/decision/topsis/runs")
    public ResponseEntity<List<TopsisDecisionRunEntity>> getTopsisDecisionRuns() {
        return ResponseEntity.ok(decisionSupportService.getAllTopsisRuns());
    }

    @GetMapping("/api/v1/decision/topsis/candidates")
    public ResponseEntity<List<RankedFleetCandidateEntity>> getRankedCandidates(@RequestParam Long runId) {
        return ResponseEntity.ok(decisionSupportService.getRankedCandidates(runId));
    }

    // Handles the frontend URL: /decision-se/topsis/rank
    @PostMapping({"/api/v1/decision/topsis/rank", "/api/v1/decision-se/topsis/rank"})
    public ResponseEntity<TopsisDecisionRunEntity> runTopsisRanking(
            @RequestParam Long bookingId,
            @RequestBody Map<String, Double> weights) {
        return ResponseEntity.ok(decisionSupportService.runTopsisRanking(bookingId, weights));
    }

    @GetMapping("/api/v1/decision/delays/history")
    public ResponseEntity<List<HarvestDelayPredictionEntity>> getDelayPredictions() {
        return ResponseEntity.ok(decisionSupportService.getAllDelayPredictions());
    }

    @PostMapping("/api/v1/decision/delays/predict")
    public ResponseEntity<HarvestDelayPredictionEntity> predictHarvestDelay(@RequestParam Long bookingId) {
        return ResponseEntity.ok(decisionSupportService.predictHarvestDelay(bookingId));
    }
}