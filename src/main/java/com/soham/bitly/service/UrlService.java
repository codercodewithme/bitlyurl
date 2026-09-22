package com.soham.bitly.service;

import com.soham.bitly.dto.ShortenRequest;
import com.soham.bitly.dto.ShortenResponse;
import com.soham.bitly.entity.ClickEvent;
import com.soham.bitly.entity.ShortUrl;
import com.soham.bitly.exception.AliasTakenException;
import com.soham.bitly.exception.ExpiredUrlException;
import com.soham.bitly.exception.InvalidUrlException;
import com.soham.bitly.exception.ResourceNotFoundException;
import com.soham.bitly.repository.ClickEventRepository;
import com.soham.bitly.repository.ShortUrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class UrlService {

    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "links", "stats", "h2-console", "error", "favicon.ico", "css", "js", "images"
    );

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final String baseUrl;

    public UrlService(ShortUrlRepository shortUrlRepository,
                      ClickEventRepository clickEventRepository,
                      ShortCodeGenerator shortCodeGenerator,
                      @Value("${app.base-url}") String baseUrl) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String originalUrl = normalizeAndValidateUrl(request.getOriginalUrl());
        boolean custom = request.getCustomAlias() != null && !request.getCustomAlias().isBlank();
        String code = custom ? request.getCustomAlias().trim() : shortCodeGenerator.generateUnique();

        if (custom) {
            if (RESERVED_ALIASES.contains(code.toLowerCase())) {
                throw new InvalidUrlException("That alias is reserved. Please choose another.");
            }
            if (shortUrlRepository.existsByShortCode(code)) {
                throw new AliasTakenException("The alias '" + code + "' is already taken.");
            }
        }

        ShortUrl entity = new ShortUrl();
        entity.setShortCode(code);
        entity.setOriginalUrl(originalUrl);
        entity.setTitle(blankToNull(request.getTitle()));
        entity.setCustomAlias(custom);
        entity.setClickCount(0);
        entity.setExpiresAt(request.getExpiresAt());
        return toResponse(shortUrlRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ShortenResponse> findAll() {
        return shortUrlRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShortenResponse findByCode(String shortCode) {
        return toResponse(requireUrl(shortCode));
    }

    @Transactional
    public void delete(String shortCode) {
        ShortUrl url = requireUrl(shortCode);
        shortUrlRepository.delete(url);
    }

    @Transactional
    public String resolveAndTrack(String shortCode, HttpServletRequest request) {
        ShortUrl url = requireUrl(shortCode);
        if (url.isExpired()) {
            throw new ExpiredUrlException("This short link has expired.");
        }

        ClickEvent click = new ClickEvent();
        click.setShortUrl(url);
        click.setIpAddress(clientIp(request));
        click.setUserAgent(trimTo(request.getHeader("User-Agent"), 512));
        click.setReferrer(trimTo(request.getHeader("Referer"), 512));
        click.setDeviceType(UserAgentParser.detectDevice(request.getHeader("User-Agent")));
        click.setBrowser(UserAgentParser.detectBrowser(request.getHeader("User-Agent")));
        clickEventRepository.save(click);

        url.setClickCount(url.getClickCount() + 1);
        shortUrlRepository.save(url);
        return url.getOriginalUrl();
    }

    public ShortUrl requireUrl(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("No short link found for '" + shortCode + "'."));
    }

    public String toShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }

    public String normalizeAndValidateUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUrlException("URL is required.");
        }

        String candidate = raw.trim();
        if (!candidate.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            candidate = "https://" + candidate;
        }

        URI uri;
        try {
            uri = URI.create(candidate);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("Enter a valid URL.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("Only http and https URLs are allowed.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("Enter a valid URL with a host.");
        }
        return candidate;
    }

    public ShortenResponse toResponse(ShortUrl entity) {
        ShortenResponse response = new ShortenResponse();
        response.setId(entity.getId());
        response.setShortCode(entity.getShortCode());
        response.setShortUrl(toShortUrl(entity.getShortCode()));
        response.setOriginalUrl(entity.getOriginalUrl());
        response.setTitle(entity.getTitle());
        response.setCustomAlias(entity.isCustomAlias());
        response.setClickCount(entity.getClickCount());
        response.setExpiresAt(entity.getExpiresAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
