package com.shortscale.controller;

import com.shortscale.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class RedirectController {

    @Autowired
    private UrlService urlService;

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        if (originalUrl == null) {
            // Return 404 or something. But RedirectView can handle.
            RedirectView redirectView = new RedirectView();
            redirectView.setStatusCode(HttpStatus.NOT_FOUND);
            return redirectView;
        }
        return new RedirectView(originalUrl);
    }
}
