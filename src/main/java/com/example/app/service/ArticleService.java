package com.example.app.service;

import com.example.app.dto.ArticleDto;
import com.example.app.dto.UserDto;
import com.example.app.entity.Article;
import com.example.app.entity.User;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.repository.ArticleRepository;
import com.example.app.repository.UserRepository;
import com.example.app.search.ArticleSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository   articleRepository;
    private final UserRepository      userRepository;
    private final ArticleSearchService searchService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public ArticleDto.Response create(ArticleDto.CreateRequest req, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        Article article = Article.builder()
                .title(req.title())
                .content(req.content())
                .status("DRAFT")
                .author(author)
                .build();
        article = articleRepository.save(article);

        // Index in Lucene
        searchService.indexArticle(article.getId(), article.getTitle(),
                article.getContent(), article.getStatus());

        return toResponse(article);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ArticleDto.Response findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ArticleDto.Response> findPublished(Pageable pageable) {
        return articleRepository.findPublished(pageable).map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public ArticleDto.Response update(UUID id, ArticleDto.UpdateRequest req, String username) {
        Article article = getOrThrow(id);
        assertOwner(article, username);

        if (req.title()   != null) article.setTitle(req.title());
        if (req.content() != null) article.setContent(req.content());
        if (req.status()  != null) article.setStatus(req.status());

        article = articleRepository.save(article);
        searchService.indexArticle(article.getId(), article.getTitle(),
                article.getContent(), article.getStatus());

        return toResponse(article);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, String username) {
        Article article = getOrThrow(id);
        assertOwner(article, username);
        articleRepository.delete(article);
        searchService.deleteArticle(id);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ArticleDto.Response> search(ArticleDto.SearchRequest req) {
        List<UUID> ids = searchService.search(req.query(), req.status(), req.maxHits());
        return articleRepository.findByIdIn(ids).stream().map(this::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Article getOrThrow(UUID id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id.toString()));
    }

    private void assertOwner(Article article, String username) {
        if (!article.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not own this article.");
        }
    }

    private ArticleDto.Response toResponse(Article a) {
        User author = a.getAuthor();
        UserDto authorDto = new UserDto(author.getId(), author.getUsername(),
                author.getEmail(), author.getFullName(), author.getRoles(),
                author.getCreatedAt());
        return new ArticleDto.Response(a.getId(), a.getTitle(), a.getContent(),
                a.getStatus(), authorDto, a.getCreatedAt(), a.getUpdatedAt());
    }
}
