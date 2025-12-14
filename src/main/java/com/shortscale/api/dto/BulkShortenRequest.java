package com.shortscale.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkShortenRequest {
    private List<ShortenRequest> requests;
}
