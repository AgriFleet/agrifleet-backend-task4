package com.agrifleet.decision.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/*
 Farmer-supplied criteria weights for a TOPSIS ranking request.
 Each weight is a preference from 0.0 (don't care) to 1.0 (only thing that matters).
 The service normalizes these to sum to 1.0 before running TOPSIS, so the
 caller doesn't have to do the math themselves.
 */
public class RankRequestDto {

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double costWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double distanceWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double horsepowerWeight;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Double ratingWeight;

    public Double getCostWeight() { return costWeight; }
    public void setCostWeight(Double costWeight) { this.costWeight = costWeight; }

    public Double getDistanceWeight() { return distanceWeight; }
    public void setDistanceWeight(Double distanceWeight) { this.distanceWeight = distanceWeight; }

    public Double getHorsepowerWeight() { return horsepowerWeight; }
    public void setHorsepowerWeight(Double horsepowerWeight) { this.horsepowerWeight = horsepowerWeight; }

    public Double getRatingWeight() { return ratingWeight; }
    public void setRatingWeight(Double ratingWeight) { this.ratingWeight = ratingWeight; }
}
