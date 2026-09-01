package com.agrifleet.decision.entity;

public class Booking {
    private long bookingId;
    private long farmerId;
    private double farmLat;
    private double farmLng;
    private double acreage;
    private String cropType;
    private String requiredWindowStart;
    private String requiredWindowEnd;
    private String bookingStatus;

    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public long getFarmerId() { return farmerId; }
    public void setFarmerId(long farmerId) { this.farmerId = farmerId; }

    public double getFarmLat() { return farmLat; }
    public void setFarmLat(double farmLat) { this.farmLat = farmLat; }

    public double getFarmLng() { return farmLng; }
    public void setFarmLng(double farmLng) { this.farmLng = farmLng; }

    public double getAcreage() { return acreage; }
    public void setAcreage(double acreage) { this.acreage = acreage; }

    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }

    public String getRequiredWindowStart() { return requiredWindowStart; }
    public void setRequiredWindowStart(String requiredWindowStart) { this.requiredWindowStart = requiredWindowStart; }

    public String getRequiredWindowEnd() { return requiredWindowEnd; }
    public void setRequiredWindowEnd(String requiredWindowEnd) { this.requiredWindowEnd = requiredWindowEnd; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
}
