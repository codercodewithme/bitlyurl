package com.soham.bitly.controller;

import com.soham.bitly.exception.ExpiredUrlException;
import com.soham.bitly.exception.ResourceNotFoundException;
import com.soham.bitly.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode:[A-Za-z0-9_-]{3,30}}")
    public String redirect(@PathVariable String shortCode,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        try {
            String destination = urlService.resolveAndTrack(shortCode, request);
            return "redirect:" + destination;
        } catch (ResourceNotFoundException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("title", "Link not found");
            model.addAttribute("message", ex.getMessage());
            return "error";
        } catch (ExpiredUrlException ex) {
            response.setStatus(HttpServletResponse.SC_GONE);
            model.addAttribute("title", "Link expired");
            model.addAttribute("message", ex.getMessage());
            return "error";
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
