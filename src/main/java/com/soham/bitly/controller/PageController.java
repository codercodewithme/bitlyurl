package com.soham.bitly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/links")
    public String links() {
        return "links";
    }

    @GetMapping("/stats/{code}")
    public String analytics(@PathVariable String code, Model model) {
        model.addAttribute("code", code);
        return "analytics";
    }
}
