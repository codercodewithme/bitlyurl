package com.soham.bitly.dto;

public class DailyClickDto {

    private String date;
    private long clicks;

    public DailyClickDto() {
    }

    public DailyClickDto(String date, long clicks) {
        this.date = date;
        this.clicks = clicks;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }
}
