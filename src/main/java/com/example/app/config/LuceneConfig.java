package com.example.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures Apache Lucene 10.x beans.
 * <p>
 * - {@link Analyzer}      → StandardAnalyzer (Unicode-aware tokenisation)
 * - {@link Directory}     → FSDirectory backed by the path from application.yml
 * - {@link IndexWriter}   → single shared writer (thread-safe)
 * - {@link IndexSearcher} → refreshed on demand via {@link LuceneSearcherFactory}
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LuceneConfig {

    private final AppProperties appProperties;

    @Bean(destroyMethod = "close")
    public Analyzer luceneAnalyzer() {
        return new StandardAnalyzer();
    }

    @Bean(destroyMethod = "close")
    public Directory luceneDirectory() throws IOException {
        Path indexPath = Paths.get(appProperties.lucene().indexPath());
        Files.createDirectories(indexPath);
        log.info("Lucene index directory: {}", indexPath.toAbsolutePath());
        return FSDirectory.open(indexPath);
    }

    @Bean(destroyMethod = "close")
    public IndexWriter luceneIndexWriter(Directory luceneDirectory, Analyzer luceneAnalyzer) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(luceneAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(luceneDirectory, config);
    }

    @Bean
    public IndexSearcher luceneIndexSearcher(Directory luceneDirectory, IndexWriter luceneIndexWriter) throws IOException {
        // Ensure at least an empty index exists
        luceneIndexWriter.commit();
        DirectoryReader reader = DirectoryReader.open(luceneDirectory);
        return new IndexSearcher(reader);
    }
}
