package com.soham.bitly.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private String title;
    private long totalClicks;
    private boolean customAlias;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<DailyClickDto> clicksByDay = new ArrayList<>();
    private Map<String, Long> clicksByDevice = new LinkedHashMap<>();
    private Map<String, Long> clicksByBrowser = new LinkedHashMap<>();
    private Map<String, Long> clicksByReferrer = new LinkedHashMap<>();
    private List<ClickEventDto> recentClicks = new ArrayList<>();

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public boolean isCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(boolean customAlias) {
        this.customAlias = customAlias;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<DailyClickDto> getClicksByDay() {
        return clicksByDay;
    }

    public void setClicksByDay(List<DailyClickDto> clicksByDay) {
        this.clicksByDay = clicksByDay;
    }

    public Map<String, Long> getClicksByDevice() {
        return clicksByDevice;
    }

    public void setClicksByDevice(Map<String, Long> clicksByDevice) {
        this.clicksByDevice = clicksByDevice;
    }

    public Map<String, Long> getClicksByBrowser() {
        return clicksByBrowser;
    }

    public void setClicksByBrowser(Map<String, Long> clicksByBrowser) {
        this.clicksByBrowser = clicksByBrowser;
    }

    public Map<String, Long> getClicksByReferrer() {
        return clicksByReferrer;
    }

    public void setClicksByReferrer(Map<String, Long> clicksByReferrer) {
        this.clicksByReferrer = clicksByReferrer;
    }

    public List<ClickEventDto> getRecentClicks() {
        return recentClicks;
    }

    public void setRecentClicks(List<ClickEventDto> recentClicks) {
        this.recentClicks = recentClicks;
    }
}
