package com.example.app.repository;

import com.example.app.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Page<Article> findByStatus(String status, Pageable pageable);

    Page<Article> findByAuthorId(UUID authorId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' ORDER BY a.createdAt DESC")
    Page<Article> findPublished(Pageable pageable);

    List<Article> findByIdIn(List<UUID> ids);
}
