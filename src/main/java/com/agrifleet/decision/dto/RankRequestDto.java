package com.agrifleet.decision.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class RankRequestDto {

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonAlias({"cost"})
    private Double costWeight;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonAlias({"distance"})
    private Double distanceWeight;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonAlias({"hp", "horsepower"})
    private Double horsepowerWeight;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @JsonAlias({"rating"})
    private Double ratingWeight;

    public Double getCostWeight() {
        return costWeight;
    }

    public void setCostWeight(Double costWeight) {
        this.costWeight = costWeight;
    }

    public Double getDistanceWeight() {
        return distanceWeight;
    }

    public void setDistanceWeight(Double distanceWeight) {
        this.distanceWeight = distanceWeight;
    }

    public Double getHorsepowerWeight() {
        return horsepowerWeight;
    }

    public void setHorsepowerWeight(Double horsepowerWeight) {
        this.horsepowerWeight = horsepowerWeight;
    }

    public Double getRatingWeight() {
        return ratingWeight;
    }

    public void setRatingWeight(Double ratingWeight) {
        this.ratingWeight = ratingWeight;
    }
}