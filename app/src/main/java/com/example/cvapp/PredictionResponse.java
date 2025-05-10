package com.example.cvapp;

public class PredictionResponse {
    private String predicted_class;
    private double confidence;
    private String status;
    private Coordinates coordinates;

    public String getPredicted_class() {
        return predicted_class;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getStatus() {
        return status;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setPredicted_class(String predicted_class) {
        this.predicted_class = predicted_class;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public static class Coordinates {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}