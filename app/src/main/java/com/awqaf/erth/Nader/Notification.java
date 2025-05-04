package com.awqaf.erth.Nader;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

public class Notification {

    public enum Status {
        APPROVED, PENDING, REJECTED
    }

    private String id;
    private String title;
    private String timestamp;
    private String secondaryInfo;
    private String statusString;
    private boolean read;
    private boolean showDetailsOption;
    private String rejectionReason;

    public Notification() {
    }

    public Notification(String id, String title, String timestamp, String secondaryInfo, Status status, boolean read, boolean showDetailsOption, String rejectionReason) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.secondaryInfo = secondaryInfo;
        this.statusString = status.name();
        this.read = read;
        this.showDetailsOption = showDetailsOption;
        if (status == Status.REJECTED) {
            this.rejectionReason = rejectionReason;
        } else {
            this.rejectionReason = null;
        }
    }

    @Exclude
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSecondaryInfo() {
        return secondaryInfo;
    }

    public String getStatusString() {
        return statusString;
    }

    @Exclude
    public Status getStatus() {
        try {
            return Status.valueOf(statusString);
        } catch (Exception e) {
            return Status.PENDING;
        }
    }

    public boolean isRead() {
        return read;
    }

    public boolean isShowDetailsOption() {
        return showDetailsOption;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSecondaryInfo(String secondaryInfo) {
        this.secondaryInfo = secondaryInfo;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    @Exclude
    public void setStatus(Status status) {
        this.statusString = status.name();
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setShowDetailsOption(boolean showDetailsOption) {
        this.showDetailsOption = showDetailsOption;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}

