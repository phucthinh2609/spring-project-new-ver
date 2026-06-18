package com.example.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        JwtProperties jwt,
        LuceneProperties lucene,
        CorsProperties cors
) {

    public record JwtProperties(
            String secret,
            long expirationMs,
            long refreshExpirationMs
    ) {}

    public record LuceneProperties(
            String indexPath
    ) {}

    public record CorsProperties(
            List<String> allowedOrigins
    ) {}
}
