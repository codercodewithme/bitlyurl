package com.soham.bitly.repository;

import com.soham.bitly.entity.ClickEvent;
import com.soham.bitly.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);

    List<ClickEvent> findByShortUrlAndClickedAtAfterOrderByClickedAtAsc(ShortUrl shortUrl, LocalDateTime after);

    long countByClickedAtAfter(LocalDateTime start);

    List<ClickEvent> findTop20ByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);
}
