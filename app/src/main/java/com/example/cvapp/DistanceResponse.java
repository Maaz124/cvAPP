package com.example.cvapp;

public class DistanceResponse {
    private double distance_meters;
    private String status;

    public double getDistance_meters() {
        return distance_meters;
    }

    public String getStatus() {
        return status;
    }

    public void setDistance_meters(double distance_meters) {
        this.distance_meters = distance_meters;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
