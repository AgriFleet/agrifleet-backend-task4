package com.agrifleet.decision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "harvest_delay_predictions")
public class HarvestDelayPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "field_acres", nullable = false)
    private Double fieldAcres;

    @Column(name = "rain_probability", nullable = false)
    private Double rainProbability;

    @Column(name = "vehicle_breakdown_history")
    private Integer vehicleBreakdownHistory;

    @Column(name = "predicted_risk_tier", nullable = false)
    private String predictedRiskTier;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;

    // Standard Getters & Setters
    public Long getPredictionId() { return predictionId; }
    public void setPredictionId(Long predictionId) { this.predictionId = predictionId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Double getFieldAcres() { return fieldAcres; }
    public void setFieldAcres(Double fieldAcres) { this.fieldAcres = fieldAcres; }
    public Double getRainProbability() { return rainProbability; }
    public void setRainProbability(Double rainProbability) { this.rainProbability = rainProbability; }
    public Integer getVehicleBreakdownHistory() { return vehicleBreakdownHistory; }
    public void setVehicleBreakdownHistory(Integer vehicleBreakdownHistory) { this.vehicleBreakdownHistory = vehicleBreakdownHistory; }
    public String getPredictedRiskTier() { return predictedRiskTier; }
    public void setPredictedRiskTier(String predictedRiskTier) { this.predictedRiskTier = predictedRiskTier; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
}