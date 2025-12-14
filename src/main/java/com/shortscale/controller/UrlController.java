package com.shortscale.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shortscale.api.dto.BulkShortenRequest;
import com.shortscale.api.dto.BulkShortenResponse;
import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;
import com.shortscale.service.UrlService;
import com.shortscale.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class UrlController {

    private final UrlService urlService;
    private final RateLimiter rateLimiter;

    public UrlController(UrlService urlService, RateLimiter rateLimiter) {
        this.urlService = urlService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimiter.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            ShortenResponse response = urlService.shortenUrl(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/shorten/bulk")
    public ResponseEntity<BulkShortenResponse> bulkShortenUrls(@RequestBody BulkShortenRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (!rateLimiter.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        try {
            BulkShortenResponse response = urlService.bulkShortenUrls(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<Integer> getAnalytics(@PathVariable String shortCode) {
        int clickCount = urlService.getClickCount(shortCode);
        return ResponseEntity.ok(clickCount);
    }

    @GetMapping(value = "/qr/{shortCode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCode(@PathVariable String shortCode) {
        try {
            String url = "http://localhost:8080/" + shortCode; // Adjust the URL as needed
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
            byte[] qrCodeImage = byteArrayOutputStream.toByteArray();

            return ResponseEntity.ok(qrCodeImage);
        } catch (WriterException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
