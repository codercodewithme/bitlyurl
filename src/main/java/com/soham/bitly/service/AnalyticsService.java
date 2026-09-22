package com.soham.bitly.service;

import com.soham.bitly.dto.AnalyticsResponse;
import com.soham.bitly.dto.ClickEventDto;
import com.soham.bitly.dto.DailyClickDto;
import com.soham.bitly.dto.DashboardStatsResponse;
import com.soham.bitly.entity.ClickEvent;
import com.soham.bitly.entity.ShortUrl;
import com.soham.bitly.repository.ClickEventRepository;
import com.soham.bitly.repository.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final UrlService urlService;

    public AnalyticsService(ShortUrlRepository shortUrlRepository,
                            ClickEventRepository clickEventRepository,
                            UrlService urlService) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.urlService = urlService;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setTotalLinks(shortUrlRepository.count());
        stats.setTotalClicks(shortUrlRepository.sumClickCount());
        stats.setLinksToday(shortUrlRepository.countByCreatedAtAfter(startOfDay));
        stats.setClicksToday(clickEventRepository.countByClickedAtAfter(startOfDay));
        stats.setActiveLinks(shortUrlRepository.countActive(LocalDateTime.now()));
        return stats;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse forCode(String shortCode) {
        ShortUrl url = urlService.requireUrl(shortCode);
        LocalDateTime windowStart = LocalDate.now().minusDays(13).atStartOfDay();
        List<ClickEvent> windowClicks = clickEventRepository
                .findByShortUrlAndClickedAtAfterOrderByClickedAtAsc(url, windowStart);
        List<ClickEvent> recent = clickEventRepository.findTop20ByShortUrlOrderByClickedAtDesc(url);

        AnalyticsResponse response = new AnalyticsResponse();
        response.setShortCode(url.getShortCode());
        response.setShortUrl(urlService.toShortUrl(url.getShortCode()));
        response.setOriginalUrl(url.getOriginalUrl());
        response.setTitle(url.getTitle());
        response.setTotalClicks(url.getClickCount());
        response.setCustomAlias(url.isCustomAlias());
        response.setExpired(url.isExpired());
        response.setCreatedAt(url.getCreatedAt());
        response.setExpiresAt(url.getExpiresAt());
        response.setClicksByDay(buildDailySeries(windowClicks));
        response.setClicksByDevice(countBy(windowClicks, ClickEvent::getDeviceType));
        response.setClicksByBrowser(countBy(windowClicks, ClickEvent::getBrowser));
        response.setClicksByReferrer(countReferrers(windowClicks));
        response.setRecentClicks(recent.stream().map(this::toClickDto).toList());
        return response;
    }

    private List<DailyClickDto> buildDailySeries(List<ClickEvent> clicks) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(13);
        for (int i = 0; i < 14; i++) {
            counts.put(start.plusDays(i), 0L);
        }
        for (ClickEvent click : clicks) {
            LocalDate day = click.getClickedAt().toLocalDate();
            counts.computeIfPresent(day, (key, value) -> value + 1);
        }
        List<DailyClickDto> series = new ArrayList<>();
        counts.forEach((day, total) -> series.add(new DailyClickDto(day.format(DAY_FORMAT), total)));
        return series;
    }

    private Map<String, Long> countBy(List<ClickEvent> clicks, java.util.function.Function<ClickEvent, String> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClickEvent click : clicks) {
            String key = extractor.apply(click);
            if (key == null || key.isBlank()) {
                key = "Unknown";
            }
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private Map<String, Long> countReferrers(List<ClickEvent> clicks) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClickEvent click : clicks) {
            String referrer = click.getReferrer();
            String key = (referrer == null || referrer.isBlank()) ? "Direct" : hostOf(referrer);
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    private String hostOf(String referrer) {
        try {
            String host = java.net.URI.create(referrer).getHost();
            return host == null ? referrer : host;
        } catch (IllegalArgumentException ex) {
            return referrer;
        }
    }

    private ClickEventDto toClickDto(ClickEvent click) {
        ClickEventDto dto = new ClickEventDto();
        dto.setClickedAt(click.getClickedAt());
        dto.setIpAddress(click.getIpAddress());
        dto.setReferrer(click.getReferrer() == null || click.getReferrer().isBlank() ? "Direct" : click.getReferrer());
        dto.setDeviceType(click.getDeviceType());
        dto.setBrowser(click.getBrowser());
        return dto;
    }
}
