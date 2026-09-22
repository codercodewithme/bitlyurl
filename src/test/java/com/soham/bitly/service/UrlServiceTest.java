package com.soham.bitly.service;

import com.soham.bitly.dto.ShortenRequest;
import com.soham.bitly.dto.ShortenResponse;
import com.soham.bitly.entity.ShortUrl;
import com.soham.bitly.exception.AliasTakenException;
import com.soham.bitly.exception.ExpiredUrlException;
import com.soham.bitly.exception.InvalidUrlException;
import com.soham.bitly.exception.ResourceNotFoundException;
import com.soham.bitly.repository.ClickEventRepository;
import com.soham.bitly.repository.ShortUrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(shortUrlRepository, clickEventRepository, shortCodeGenerator, "http://localhost:8080");
    }

    @Test
    void shortensUrlAndAddsHttpsWhenMissing() {
        when(shortCodeGenerator.generateUnique()).thenReturn("abc1234");
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("spring.io");
        request.setTitle("Spring");

        ShortenResponse response = urlService.shorten(request);

        assertEquals("https://spring.io", response.getOriginalUrl());
        assertEquals("http://localhost:8080/abc1234", response.getShortUrl());
        assertEquals("Spring", response.getTitle());
    }

    @Test
    void rejectsReservedAlias() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("api");

        assertThrows(InvalidUrlException.class, () -> urlService.shorten(request));
    }

    @Test
    void rejectsTakenAlias() {
        when(shortUrlRepository.existsByShortCode("launch")).thenReturn(true);

        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("launch");

        assertThrows(AliasTakenException.class, () -> urlService.shorten(request));
    }

    @Test
    void rejectsJavascriptUrl() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("javascript:alert(1)");

        assertThrows(InvalidUrlException.class, () -> urlService.shorten(request));
    }

    @Test
    void tracksClickAndReturnsDestination() {
        ShortUrl url = new ShortUrl();
        url.setShortCode("wiki");
        url.setOriginalUrl("https://en.wikipedia.org");
        url.setClickCount(2);
        when(shortUrlRepository.findByShortCode("wiki")).thenReturn(Optional.of(url));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Referer")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String destination = urlService.resolveAndTrack("wiki", request);

        assertEquals("https://en.wikipedia.org", destination);
        assertEquals(3, url.getClickCount());
        verify(clickEventRepository).save(any());
    }

    @Test
    void expiredLinkIsRejected() {
        ShortUrl url = new ShortUrl();
        url.setShortCode("old");
        url.setOriginalUrl("https://example.com");
        url.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(shortUrlRepository.findByShortCode("old")).thenReturn(Optional.of(url));

        assertThrows(ExpiredUrlException.class,
                () -> urlService.resolveAndTrack("old", mock(HttpServletRequest.class)));
    }

    @Test
    void missingLinkThrowsNotFound() {
        when(shortUrlRepository.findByShortCode("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> urlService.findByCode("nope"));
    }

    @Test
    void savesCustomAlias() {
        when(shortUrlRepository.existsByShortCode("launch")).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com/campaign");
        request.setCustomAlias("launch");

        urlService.shorten(request);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        assertEquals("launch", captor.getValue().getShortCode());
        assertTrue(captor.getValue().isCustomAlias());
    }
}
