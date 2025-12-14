package com.shortscale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shortscale.api.dto.BulkShortenRequest;
import com.shortscale.api.dto.BulkShortenResponse;
import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;
import com.shortscale.service.UrlService;
import com.shortscale.util.RateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
public class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @MockBean
    private RateLimiter rateLimiter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldShortenUrlSuccessfully() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        ShortenResponse response = new ShortenResponse();
        response.setShortUrl("http://localhost:8080/abc");
        response.setOriginalUrl("https://example.com");
        response.setShortCode("abc");

        when(rateLimiter.isAllowed(any())).thenReturn(true);
        when(urlService.shortenUrl(any(ShortenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc"));
    }

    @Test
    public void shouldReturnTooManyRequestsWhenRateLimited() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");

        when(rateLimiter.isAllowed(any())).thenReturn(false);

        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void shouldReturnBadRequestWhenInvalid() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("invalid");

        when(rateLimiter.isAllowed(any())).thenReturn(true);
        when(urlService.shortenUrl(any(ShortenRequest.class))).thenThrow(new IllegalArgumentException());

        mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldBulkShortenUrlsSuccessfully() throws Exception {
        BulkShortenRequest request = new BulkShortenRequest();
        ShortenRequest sr = new ShortenRequest();
        sr.setOriginalUrl("https://example.com");
        request.setRequests(List.of(sr));

        BulkShortenResponse response = new BulkShortenResponse();
        ShortenResponse srResp = new ShortenResponse();
        srResp.setShortUrl("http://localhost:8080/abc");
        response.setResponses(List.of(srResp));

        when(rateLimiter.isAllowed(any())).thenReturn(true);
        when(urlService.bulkShortenUrls(any(BulkShortenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shorten/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnTooManyRequestsForBulkWhenRateLimited() throws Exception {
        BulkShortenRequest request = new BulkShortenRequest();
        ShortenRequest sr = new ShortenRequest();
        sr.setOriginalUrl("https://example.com");
        request.setRequests(List.of(sr));

        when(rateLimiter.isAllowed(any())).thenReturn(false);

        mockMvc.perform(post("/api/shorten/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void shouldReturnBadRequestForBulkWhenInvalid() throws Exception {
        BulkShortenRequest request = new BulkShortenRequest();
        ShortenRequest sr = new ShortenRequest();
        sr.setOriginalUrl("invalid");
        request.setRequests(List.of(sr));

        when(rateLimiter.isAllowed(any())).thenReturn(true);
        when(urlService.bulkShortenUrls(any(BulkShortenRequest.class))).thenThrow(new IllegalArgumentException());

        mockMvc.perform(post("/api/shorten/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnClickCountForAnalytics() throws Exception {
        when(urlService.getClickCount("abc")).thenReturn(5);

        mockMvc.perform(get("/api/analytics/abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    public void shouldReturnQRCodeImage() throws Exception {
        mockMvc.perform(get("/api/qr/abc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    public void shouldReturnInternalServerErrorWhenQRCodeFails() throws Exception {
        try (MockedConstruction<QRCodeWriter> mocked = Mockito.mockConstruction(QRCodeWriter.class, (mock, context) -> {
            when(mock.encode(anyString(), any(), anyInt(), anyInt())).thenThrow(new WriterException("Test exception"));
        })) {
            mockMvc.perform(get("/api/qr/abc"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
