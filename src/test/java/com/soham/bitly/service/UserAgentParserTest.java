package com.soham.bitly.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentParserTest {

    @Test
    void detectsMobileChrome() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1";
        assertEquals("Mobile", UserAgentParser.detectDevice(ua));
        assertEquals("Safari", UserAgentParser.detectBrowser(ua));
    }

    @Test
    void detectsDesktopEdge() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Edg/120.0.0.0";
        assertEquals("Desktop", UserAgentParser.detectDevice(ua));
        assertEquals("Edge", UserAgentParser.detectBrowser(ua));
    }

    @Test
    void handlesBlankAgent() {
        assertEquals("Unknown", UserAgentParser.detectDevice(""));
        assertEquals("Unknown", UserAgentParser.detectBrowser(null));
    }
}
