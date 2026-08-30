package com.agrifleet.decision.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;


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
