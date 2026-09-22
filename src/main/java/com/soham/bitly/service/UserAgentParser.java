package com.soham.bitly.service;

public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static String detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("playbook")) {
            return "Tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "Mobile";
        }
        return "Desktop";
    }

    public static String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return "Opera";
        }
        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            return "Safari";
        }
        return "Other";
    }
}
