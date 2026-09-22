package com.soham.bitly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must be 2048 characters or fewer")
    private String originalUrl;

    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,30}$",
            message = "Custom alias must be 3-30 characters using letters, numbers, _ or -")
    private String customAlias;

    private LocalDateTime expiresAt;

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

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
