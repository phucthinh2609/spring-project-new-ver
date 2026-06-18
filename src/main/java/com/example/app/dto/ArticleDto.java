package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class ArticleDto {

	//To response API
    public record Response(
            UUID id,
            String title,
            String content,
            String status,
            UserDto author,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateRequest(
            @NotBlank @Size(max = 255) String title,
            String content
    ) {}

    public record UpdateRequest(
            @Size(max = 255) String title,
            String content,
            String status
    ) {}

    public record SearchRequest(
            @NotBlank String query,
            String status,
            int maxHits
    ) {
        public SearchRequest {
            if (maxHits <= 0) maxHits = 20;
        }
    }
}
