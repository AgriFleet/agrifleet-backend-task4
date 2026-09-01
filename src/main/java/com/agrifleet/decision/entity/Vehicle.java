package com.agrifleet.decision.entity;

import com.fasterxml.jackson.databind.JsonNode;

public class Vehicle {
    private long vehicleId;
    private long ownerId;
    private String vehicleType;
    private JsonNode specs;      // parsed from the `specs` JSON column
    private JsonNode pricing;    // parsed from the `pricing` JSON column
    private double rating;
    private double currentLat;
    private double currentLng;
    private String availabilityStatus;

    // convenience accessors for the fields TOPSIS actually needs

    public double hourlyRate() {
        return pricing != null && pricing.has("hourly_rate") ? pricing.get("hourly_rate").asDouble() : 0.0;
    }

    public int horsepower() {
        return specs != null && specs.has("hp") ? specs.get("hp").asInt() : 0;
    }

    //  getters / setters

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public JsonNode getSpecs() { return specs; }
    public void setSpecs(JsonNode specs) { this.specs = specs; }

    public JsonNode getPricing() { return pricing; }
    public void setPricing(JsonNode pricing) { this.pricing = pricing; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public double getCurrentLat() { return currentLat; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }

    public double getCurrentLng() { return currentLng; }
    public void setCurrentLng(double currentLng) { this.currentLng = currentLng; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}
