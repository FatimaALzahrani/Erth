package com.awqaf.erth.model;


import java.util.HashMap;
import java.util.Map;

public class Waqf {
    public String waqfId;
    public String userId;
    public String waqfName;
    public String description;
    public String goal;
    public String path;
    public String locationName;
    public double latitude;
    public double longitude;
    public String dateFrom;
    public String dateTo;
    public double totalAmount;
    public double selfFundingPercentage;
    public double selfFundingAmount;
    public double requiredFundingPercentage;
    public double requiredFundingAmount;
    public boolean registeredWithAuthority;
    public boolean hasMortgage;
    public String status;
    public long createdAtTimestamp;
    public Double paidAmount;

    // File/Image URLs
    public String waqfImageUrl;
    public String waqfDocUrl;

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String feasibilityStudyUrl;
    public String municipalLicenseUrl;
    public String commercialRegisterUrl;
    public String additionalDocsUrl;

    public Waqf() {
    }

    public Waqf(String waqfId, String userId, String waqfName, String description, String goal, String path,
                String locationName, double latitude, double longitude, String dateFrom, String dateTo,
                double totalAmount, double selfFundingPercentage, double selfFundingAmount,
                double requiredFundingPercentage, double requiredFundingAmount, boolean registeredWithAuthority,
                boolean hasMortgage, String status, long createdAtTimestamp,
                String waqfImageUrl, // Added parameter
                String waqfDocUrl, String feasibilityStudyUrl, String municipalLicenseUrl,
                String commercialRegisterUrl, String additionalDocsUrl) {
        this.waqfId = waqfId;
        this.userId = userId;
        this.waqfName = waqfName;
        this.description = description;
        this.goal = goal;
        this.path = path;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.totalAmount = totalAmount;
        this.selfFundingPercentage = selfFundingPercentage;
        this.selfFundingAmount = selfFundingAmount;
        this.requiredFundingPercentage = requiredFundingPercentage;
        this.requiredFundingAmount = requiredFundingAmount;
        this.registeredWithAuthority = registeredWithAuthority;
        this.hasMortgage = hasMortgage;
        this.status = status;
        this.createdAtTimestamp = createdAtTimestamp;
        this.waqfImageUrl = waqfImageUrl; // Assign parameter
        this.waqfDocUrl = waqfDocUrl;
        this.feasibilityStudyUrl = feasibilityStudyUrl;
        this.municipalLicenseUrl = municipalLicenseUrl;
        this.commercialRegisterUrl = commercialRegisterUrl;
        this.additionalDocsUrl = additionalDocsUrl;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("waqfId", waqfId);
        result.put("userId", userId);
        result.put("waqfName", waqfName);
        result.put("description", description);
        result.put("goal", goal);
        result.put("path", path);
        result.put("locationName", locationName);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("dateFrom", dateFrom);
        result.put("dateTo", dateTo);
        result.put("totalAmount", totalAmount);
        result.put("selfFundingPercentage", selfFundingPercentage);
        result.put("selfFundingAmount", selfFundingAmount);
        result.put("requiredFundingPercentage", requiredFundingPercentage);
        result.put("requiredFundingAmount", requiredFundingAmount);
        result.put("registeredWithAuthority", registeredWithAuthority);
        result.put("hasMortgage", hasMortgage);
        result.put("status", status);
        result.put("createdAtTimestamp", createdAtTimestamp);
        result.put("waqfImageUrl", waqfImageUrl); // Added field to map
        result.put("waqfDocUrl", waqfDocUrl);
        result.put("feasibilityStudyUrl", feasibilityStudyUrl);
        result.put("municipalLicenseUrl", municipalLicenseUrl);
        result.put("commercialRegisterUrl", commercialRegisterUrl);
        result.put("additionalDocsUrl", additionalDocsUrl);
        return result;
    }
}

