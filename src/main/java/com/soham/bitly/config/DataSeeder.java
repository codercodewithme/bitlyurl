package com.soham.bitly.config;

import com.soham.bitly.entity.ClickEvent;
import com.soham.bitly.entity.ShortUrl;
import com.soham.bitly.repository.ClickEventRepository;
import com.soham.bitly.repository.ShortUrlRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    public DataSeeder(ShortUrlRepository shortUrlRepository, ClickEventRepository clickEventRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Override
    public void run(String... args) {
        if (shortUrlRepository.count() > 0) {
            return;
        }

        ShortUrl docs = saveLink("spring", "https://spring.io/projects/spring-boot",
                "Spring Boot", true, null, 4);
        ShortUrl wiki = saveLink("wiki", "https://en.wikipedia.org/wiki/URL_shortening",
                "URL shortening", true, LocalDateTime.now().plusDays(30), 2);
        saveLink("demo7k", "https://github.com", "GitHub", false, null, 0);

        seedClick(docs, LocalDateTime.now().minusDays(2), "Desktop", "Chrome", "");
        seedClick(docs, LocalDateTime.now().minusDays(1), "Mobile", "Safari", "https://twitter.com");
        seedClick(docs, LocalDateTime.now().minusHours(5), "Desktop", "Edge", "");
        seedClick(docs, LocalDateTime.now().minusHours(1), "Desktop", "Chrome", "https://linkedin.com");
        seedClick(wiki, LocalDateTime.now().minusDays(3), "Desktop", "Firefox", "");
        seedClick(wiki, LocalDateTime.now().minusHours(8), "Tablet", "Chrome", "https://reddit.com");
    }

    private ShortUrl saveLink(String code, String original, String title, boolean custom,
                              LocalDateTime expiresAt, long clicks) {
        ShortUrl url = new ShortUrl();
        url.setShortCode(code);
        url.setOriginalUrl(original);
        url.setTitle(title);
        url.setCustomAlias(custom);
        url.setClickCount(clicks);
        url.setExpiresAt(expiresAt);
        return shortUrlRepository.save(url);
    }

    private void seedClick(ShortUrl url, LocalDateTime when, String device, String browser, String referrer) {
        ClickEvent click = new ClickEvent();
        click.setShortUrl(url);
        click.setClickedAt(when);
        click.setIpAddress("127.0.0.1");
        click.setUserAgent("Seed/" + browser);
        click.setReferrer(referrer);
        click.setDeviceType(device);
        click.setBrowser(browser);
        clickEventRepository.save(click);
    }
}
