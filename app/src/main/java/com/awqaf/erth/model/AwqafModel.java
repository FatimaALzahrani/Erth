package com.awqaf.erth.model;

public class AwqafModel {
    private String name;
    private String version;
    private String imageUrl;
    private int progress;
    private double minValue;
    private double maxValue;

    private String userId;
    private String status;
    private double amountPaid;

    public AwqafModel() {}

    public AwqafModel(String name, String version, String imageUrl, int progress, double minValue, double maxValue, String userId, String status, double amountPaid) {
        this.name = name;
        this.version = version;
        this.imageUrl = imageUrl;
        this.progress = progress;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.userId = userId;
        this.status = status;
        this.amountPaid = amountPaid;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getImageUrl() { return imageUrl; }
    public int getProgress() { return progress; }
    public double getMinValue() { return minValue; }
    public double getMaxValue() { return maxValue; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
}
