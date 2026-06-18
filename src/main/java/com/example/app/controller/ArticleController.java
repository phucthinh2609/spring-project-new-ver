package com.example.app.controller;

import com.example.app.dto.ArticleDto;
import com.example.app.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<ArticleDto.Response>> listPublished(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(articleService.findPublished(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto.Response> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(articleService.findById(id));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @PostMapping("/search")
    public ResponseEntity<List<ArticleDto.Response>> search(
            @Valid @RequestBody ArticleDto.SearchRequest request) {
        return ResponseEntity.ok(articleService.search(request));
    }

    // ── Protected endpoints ───────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ArticleDto.Response> create(
            @Valid @RequestBody ArticleDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.create(request, principal.getUsername()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ArticleDto.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody ArticleDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(articleService.update(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        articleService.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
