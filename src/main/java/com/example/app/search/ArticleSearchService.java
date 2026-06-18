package com.example.app.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps Apache Lucene 10.x for full-text indexing and searching of Articles.
 *
 * <p>Fields indexed per document:
 * <ul>
 *   <li>{@code id}      – stored, not analysed (exact match / delete by ID)</li>
 *   <li>{@code title}   – stored + analysed (boosted x2)</li>
 *   <li>{@code content} – stored + analysed</li>
 *   <li>{@code status}  – stored, not analysed (filter term)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleSearchService {

    // ── Field names ────────────────────────────────────────────────────────────
    public static final String FIELD_ID      = "id";
    public static final String FIELD_TITLE   = "title";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_STATUS  = "status";

    private final IndexWriter  indexWriter;
    private final Directory    luceneDirectory;
    private final Analyzer     luceneAnalyzer;

    // ── Indexing ──────────────────────────────────────────────────────────────

    /**
     * Upserts a Lucene document for the given article.
     */
    public void indexArticle(UUID id, String title, String content, String status) {
        try {
            Document doc = buildDocument(id, title, content, status);
            indexWriter.updateDocument(new Term(FIELD_ID, id.toString()), doc);
            indexWriter.commit();
            log.debug("Lucene: indexed article {}", id);
        } catch (IOException e) {
            log.error("Lucene indexing failed for article {}: {}", id, e.getMessage(), e);
        }
    }

    /**
     * Removes an article from the index.
     */
    public void deleteArticle(UUID id) {
        try {
            indexWriter.deleteDocuments(new Term(FIELD_ID, id.toString()));
            indexWriter.commit();
            log.debug("Lucene: deleted article {}", id);
        } catch (IOException e) {
            log.error("Lucene delete failed for article {}: {}", id, e.getMessage(), e);
        }
    }

    // ── Searching ─────────────────────────────────────────────────────────────

    /**
     * Full-text search across title + content, optionally filtered by status.
     *
     * @param queryText free-text query
     * @param status    optional status filter (null → all)
     * @param maxHits   maximum number of results
     * @return list of article UUIDs ordered by relevance score
     */
    public List<UUID> search(String queryText, String status, int maxHits) {
        try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            Query textQuery = buildTextQuery(queryText);
            Query finalQuery = status != null
                    ? new BooleanQuery.Builder()
                            .add(textQuery, BooleanClause.Occur.MUST)
                            .add(new TermQuery(new Term(FIELD_STATUS, status)), BooleanClause.Occur.FILTER)
                            .build()
                    : textQuery;

            TopDocs topDocs = searcher.search(finalQuery, maxHits);
            List<UUID> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(UUID.fromString(doc.get(FIELD_ID)));
            }
            log.debug("Lucene search '{}' → {} hits", queryText, results.size());
            return results;

        } catch (IOException | ParseException e) {
            log.error("Lucene search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Document buildDocument(UUID id, String title, String content, String status) {
        Document doc = new Document();
        // Exact-match stored field (used as update key)
        doc.add(new StringField(FIELD_ID, id.toString(), Field.Store.YES));
        // Analysed + stored text fields
        doc.add(new TextField(FIELD_TITLE,   title   != null ? title   : "", Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, content != null ? content : "", Field.Store.NO));
        // Keyword for filter
        doc.add(new StringField(FIELD_STATUS, status != null ? status : "", Field.Store.YES));
        return doc;
    }

    private Query buildTextQuery(String queryText) throws ParseException {

        // Lucene 10 removed setFieldsBoost(); pass the boosts map directly
        // into the 3-argument constructor instead.
        Map<String, Float> boosts = Map.of(
                FIELD_TITLE,   2.0f,
                FIELD_CONTENT, 1.0f
        );
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{FIELD_TITLE, FIELD_CONTENT}, luceneAnalyzer, boosts);
        parser.setDefaultOperator(QueryParserBase.AND_OPERATOR);
        return parser.parse(QueryParserBase.escape(queryText));
    }
}
