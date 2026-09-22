package com.soham.bitly.dto;

public class DashboardStatsResponse {

    private long totalLinks;
    private long totalClicks;
    private long linksToday;
    private long clicksToday;
    private long activeLinks;

    public long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public long getLinksToday() {
        return linksToday;
    }

    public void setLinksToday(long linksToday) {
        this.linksToday = linksToday;
    }

    public long getClicksToday() {
        return clicksToday;
    }

    public void setClicksToday(long clicksToday) {
        this.clicksToday = clicksToday;
    }

    public long getActiveLinks() {
        return activeLinks;
    }

    public void setActiveLinks(long activeLinks) {
        this.activeLinks = activeLinks;
    }
}
