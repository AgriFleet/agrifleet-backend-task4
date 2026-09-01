package com.agrifleet.decision.controller;

import com.agrifleet.decision.dto.RankRequestDto;
import com.agrifleet.decision.dto.RankingResponseDto;
import com.agrifleet.decision.dto.RiskPredictionResponseDto;
import com.agrifleet.decision.service.DecisionService;
import com.agrifleet.decision.service.RiskPredictionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decision")
@CrossOrigin(origins = "*") // tighten to the React dev server origin before deployment
public class DecisionController {

    private final DecisionService decisionService;
    private final RiskPredictionService riskPredictionService;

    public DecisionController(DecisionService decisionService, RiskPredictionService riskPredictionService) {
        this.decisionService = decisionService;
        this.riskPredictionService = riskPredictionService;
    }

    /*
      Ranks available, crop-compatible machinery for a booking using TOPSIS,
      with farmer-supplied preference weights.
     */
    @PostMapping("/rank/{bookingId}")
    public RankingResponseDto rankVehicles(@PathVariable long bookingId, @Valid @RequestBody RankRequestDto request) {
        return decisionService.rankVehiclesForBooking(bookingId, request);
    }

    /*
      Predicts the harvest delay risk tier for a booking using the decision-tree model.
      rainProbability and breakdownHistory are optional; sensible defaults are used if omitted.
     */
    @PostMapping("/risk/{bookingId}")
    public RiskPredictionResponseDto predictRisk(
            @PathVariable long bookingId,
            @RequestParam(defaultValue = "0.3") double rainProbability,
            @RequestParam(defaultValue = "0") int breakdownHistory) {
        return riskPredictionService.predictAndPersist(bookingId, rainProbability, breakdownHistory);
    }
}
