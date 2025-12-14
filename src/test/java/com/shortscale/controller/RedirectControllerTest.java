package com.shortscale.controller;

import com.shortscale.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
public class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Test
    public void testRedirectFound() throws Exception {
        when(urlService.getOriginalUrl("abc")).thenReturn("http://example.com");

        mockMvc.perform(get("/abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://example.com"));
    }

    @Test
    public void testRedirectNotFound() throws Exception {
        when(urlService.getOriginalUrl("abc")).thenReturn(null);

        mockMvc.perform(get("/abc"))
                .andExpect(status().isNotFound());
    }
}
