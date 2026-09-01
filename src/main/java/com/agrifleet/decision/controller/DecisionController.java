package com.agrifleet.decision.controller;

import com.agrifleet.decision.dto.RankRequestDto;
import com.agrifleet.decision.dto.RankingResponseDto;
import com.agrifleet.decision.dto.RiskPredictionResponseDto;
import com.agrifleet.decision.repository.DecisionRunRepository;
import com.agrifleet.decision.repository.HarvestDelayPredictionRepository;
import com.agrifleet.decision.service.DecisionService;
import com.agrifleet.decision.service.RiskPredictionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/decision")
@CrossOrigin(origins = "*")
public class DecisionController {

    private final DecisionService decisionService;
    private final RiskPredictionService riskPredictionService;
    private final DecisionRunRepository decisionRunRepository;
    private final HarvestDelayPredictionRepository predictionRepository;

    public DecisionController(
            DecisionService decisionService,
            RiskPredictionService riskPredictionService,
            DecisionRunRepository decisionRunRepository,
            HarvestDelayPredictionRepository predictionRepository) {

        this.decisionService = decisionService;
        this.riskPredictionService = riskPredictionService;
        this.decisionRunRepository = decisionRunRepository;
        this.predictionRepository = predictionRepository;
    }

    @PostMapping("/decision-se/topsis/rank")
    public RankingResponseDto rankVehicles(
            @RequestParam long bookingId,
            @Valid @RequestBody RankRequestDto request) {

        return decisionService.rankVehiclesForBooking(
                bookingId,
                request
        );
    }

    @GetMapping("/decision/topsis/runs")
    public List<Map<String, Object>> getTopsisRuns() {
        return decisionRunRepository.findAllRuns();
    }


    @GetMapping("/decision/topsis/candidates")
    public List<Map<String, Object>> getRankedCandidates(
            @RequestParam long runId) {

        return decisionRunRepository.findCandidatesByRunId(runId);
    }

    @PostMapping("/decision/delays/predict")
    public RiskPredictionResponseDto predictHarvestDelay(
            @RequestParam long bookingId) {

        return riskPredictionService.predictFromStoredData(bookingId);
    }

    @GetMapping("/decision/delays/history")
    public List<Map<String, Object>> getDelayHistory() {
        return predictionRepository.findAll();
    }
}