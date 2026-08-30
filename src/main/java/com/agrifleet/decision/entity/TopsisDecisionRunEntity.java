package com.agrifleet.decision.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topsis_decision_runs")
public class TopsisDecisionRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_run_id")
    private Long decisionRunId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "criteria_weights", nullable = false)
    private String criteriaWeights;

    @Column(name = "ideal_best_vector_a_plus", nullable = false)
    private String idealBestVectorAPlus;

    @Column(name = "ideal_worst_vector_a_minus", nullable = false)
    private String idealWorstVectorAMinus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;

    // Standard Getters & Setters
    public Long getDecisionRunId() { return decisionRunId; }
    public void setDecisionRunId(Long decisionRunId) { this.decisionRunId = decisionRunId; }
    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getCriteriaWeights() { return criteriaWeights; }
    public void setCriteriaWeights(String criteriaWeights) { this.criteriaWeights = criteriaWeights; }
    public String getIdealBestVectorAPlus() { return idealBestVectorAPlus; }
    public void setIdealBestVectorAPlus(String idealBestVectorAPlus) { this.idealBestVectorAPlus = idealBestVectorAPlus; }
    public String getIdealWorstVectorAMinus() { return idealWorstVectorAMinus; }
    public void setIdealWorstVectorAMinus(String idealWorstVectorAMinus) { this.idealWorstVectorAMinus = idealWorstVectorAMinus; }
}