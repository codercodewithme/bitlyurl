package com.soham.bitly.controller.api;

import com.soham.bitly.dto.AnalyticsResponse;
import com.soham.bitly.dto.DashboardStatsResponse;
import com.soham.bitly.dto.ShortenRequest;
import com.soham.bitly.dto.ShortenResponse;
import com.soham.bitly.service.AnalyticsService;
import com.soham.bitly.service.QrCodeService;
import com.soham.bitly.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UrlApiController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final QrCodeService qrCodeService;

    public UrlApiController(UrlService urlService,
                            AnalyticsService analyticsService,
                            QrCodeService qrCodeService) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/urls")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.ok(urlService.shorten(request));
    }

    @GetMapping("/urls")
    public List<ShortenResponse> list() {
        return urlService.findAll();
    }

    @GetMapping("/urls/{code}")
    public ShortenResponse get(@PathVariable String code) {
        return urlService.findByCode(code);
    }

    @DeleteMapping("/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        urlService.delete(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/urls/{code}/analytics")
    public AnalyticsResponse analytics(@PathVariable String code) {
        return analyticsService.forCode(code);
    }

    @GetMapping(value = "/urls/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String code) {
        ShortenResponse url = urlService.findByCode(code);
        byte[] png = qrCodeService.generatePng(url.getShortUrl(), 320);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(png);
    }

    @GetMapping("/stats/summary")
    public DashboardStatsResponse summary() {
        return analyticsService.dashboard();
    }
}
