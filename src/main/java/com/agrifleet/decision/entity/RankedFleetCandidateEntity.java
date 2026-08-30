package com.agrifleet.decision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ranked_fleet_candidates")
public class RankedFleetCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long rankingId;

    @Column(name = "decision_run_id", nullable = false)
    private Long decisionRunId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "hourly_rate", nullable = false)
    private Double hourlyRate;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "horsepower", nullable = false)
    private Integer horsepower;

    @Column(name = "rating_score", nullable = false)
    private Double ratingScore;

    @Column(name = "separation_s_plus", nullable = false)
    private Double separationSPlus;

    @Column(name = "separation_s_minus", nullable = false)
    private Double separationSMinus;

    @Column(name = "relative_closeness_c", nullable = false)
    private Double relativeClosenessC;

    @Column(name = "final_rank", nullable = false)
    private Integer finalRank;

    // Standard Getters & Setters
    public Long getRankingId() { return rankingId; }
    public void setRankingId(Long rankingId) { this.rankingId = rankingId; }
    public Long getDecisionRunId() { return decisionRunId; }
    public void setDecisionRunId(Long decisionRunId) { this.decisionRunId = decisionRunId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getHorsepower() { return horsepower; }
    public void setHorsepower(Integer horsepower) { this.horsepower = horsepower; }
    public Double getRatingScore() { return ratingScore; }
    public void setRatingScore(Double ratingScore) { this.ratingScore = ratingScore; }
    public Double getSeparationSPlus() { return separationSPlus; }
    public void setSeparationSPlus(Double separationSPlus) { this.separationSPlus = separationSPlus; }
    public Double getSeparationSMinus() { return separationSMinus; }
    public void setSeparationSMinus(Double separationSMinus) { this.separationSMinus = separationSMinus; }
    public Double getRelativeClosenessC() { return relativeClosenessC; }
    public void setRelativeClosenessC(Double relativeClosenessC) { this.relativeClosenessC = relativeClosenessC; }
    public Integer getFinalRank() { return finalRank; }
    public void setFinalRank(Integer finalRank) { this.finalRank = finalRank; }
}