package com.soham.bitly.repository;

import com.soham.bitly.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<ShortUrl> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(LocalDateTime start);

    @Query("select coalesce(sum(s.clickCount), 0) from ShortUrl s")
    long sumClickCount();

    @Query("select count(s) from ShortUrl s where s.expiresAt is null or s.expiresAt > :now")
    long countActive(LocalDateTime now);
}
