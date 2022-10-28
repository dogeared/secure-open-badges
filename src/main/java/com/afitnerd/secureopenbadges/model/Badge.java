package com.afitnerd.secureopenbadges.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class Badge {

    @JsonProperty("badge-id")
    private String badgeSlug;

    @JsonProperty("start-date")
    private Date startDate;

    @JsonProperty("end-date")
    private Date endDate;

    public String getBadgeSlug() {
        return badgeSlug;
    }

    public void setBadgeSlug(String badgeSlug) {
        this.badgeSlug = badgeSlug;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        return
            (!(o instanceof Badge badge)) ||
            badge == this ||
            (badge.getBadgeSlug() == null && this.badgeSlug == null) ||
            (badge.getBadgeSlug() != null && badge.getBadgeSlug().equals(this.badgeSlug));
    }
}
