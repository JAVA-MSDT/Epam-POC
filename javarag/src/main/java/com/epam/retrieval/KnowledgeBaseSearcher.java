package com.epam.retrieval;

import com.epam.model.KnowledgeEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Searches the indexed knowledge base for entries relevant to analysis findings.
 * Uses Lucene to perform fast text-based searches across knowledge entries.
 */
public class KnowledgeBaseSearcher {
    private final String indexDirPath;

    /**
     * Creates a new knowledge base searcher.
     * 
     * @param indexDirPath Path to the Lucene index directory
     */
    public KnowledgeBaseSearcher(String indexDirPath) {
        this.indexDirPath = indexDirPath;
    }

    /**
     * Searches the knowledge base for entries matching the query string.
     * 
     * @param queryStr Search query (typically from analysis finding)
     * @param maxResults Maximum number of results to return
     * @return List of matching knowledge entries, ordered by relevance
     * @throws Exception If search fails due to index or query issues
     */
    public List<KnowledgeEntry> search(String queryStr, int maxResults) throws Exception {
        List<KnowledgeEntry> results = new ArrayList<>();
        
        Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
        try (DirectoryReader reader = DirectoryReader.open(indexDir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();

            // Debug: Print what we're searching for
            System.out.println("    Searching knowledge base for: '" + queryStr + "'");
            
            // Search across multiple fields for better matching
            Query query = buildMultiFieldQuery(queryStr, analyzer);
            
            TopDocs topDocs = searcher.search(query, maxResults);
            System.out.println("    Found " + topDocs.totalHits.value + " potential matches");
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                KnowledgeEntry entry = documentToKnowledgeEntry(doc);
                System.out.println("    Match: " + entry.getTitle() + " (score: " + scoreDoc.score + ")");
                results.add(entry);
            }
        }
        
        return results;
    }

    /**
     * Gets all available knowledge base topics for suggestions.
     * 
     * @return List of available knowledge base topic titles
     * @throws Exception If search fails
     */
    public List<String> getAllTopics() throws Exception {
        List<String> topics = new ArrayList<>();
        
        Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
        try (DirectoryReader reader = DirectoryReader.open(indexDir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // Get all documents
            Query query = new MatchAllDocsQuery();
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String title = doc.get("title");
                if (title != null) {
                    topics.add(title);
                }
            }
        }
        
        return topics;
    }

    /**
     * Builds a multi-field query to search across title, description, and tags.
     * Enhanced to match common static analysis rule names with knowledge base entries.
     * 
     * @param queryStr The search query string
     * @param analyzer Lucene analyzer for query processing
     * @return Lucene Query object for searching
     * @throws Exception If query parsing fails
     */
    private Query buildMultiFieldQuery(String queryStr, Analyzer analyzer) throws Exception {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        
        // Normalize query string
        String normalizedQuery = queryStr.toLowerCase();
        
        // Search in multiple fields with different strategies
        String[] fields = {"title", "description", "tags"};
        
        for (String field : fields) {
            // Exact term search
            Query exactQuery = new TermQuery(new Term(field, normalizedQuery));
            booleanQuery.add(exactQuery, BooleanClause.Occur.SHOULD);
            
            // Partial match with wildcards
            if (normalizedQuery.length() > 2) {
                Query wildcardQuery = new WildcardQuery(new Term(field, "*" + normalizedQuery + "*"));
                booleanQuery.add(wildcardQuery, BooleanClause.Occur.SHOULD);
            }
            
            // Handle common rule name patterns
            addPatternQueries(booleanQuery, field, normalizedQuery);
        }
        
        return booleanQuery.build();
    }
    
    /**
     * Adds pattern-based queries to improve matching between rule names and knowledge entries.
     * 
     * @param booleanQuery The boolean query builder
     * @param field The field to search in
     * @param queryStr The normalized query string
     */
    private void addPatternQueries(BooleanQuery.Builder booleanQuery, String field, String queryStr) {
        // Map common static analysis rule patterns to knowledge base terms
        if (queryStr.contains("vector") || queryStr.contains("legacy collection") || queryStr.contains("avoivector")) {
            booleanQuery.add(new TermQuery(new Term(field, "vector")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "arraylist")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "legacy")), BooleanClause.Occur.SHOULD);
        }
        
        if (queryStr.contains("enumeration") || queryStr.contains("elements") || queryStr.contains("iterator")) {
            booleanQuery.add(new TermQuery(new Term(field, "enumeration")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "iterator")), BooleanClause.Occur.SHOULD);
        }
        
        if (queryStr.contains("synchronized") || queryStr.contains("concurrent")) {
            booleanQuery.add(new TermQuery(new Term(field, "synchronized")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "concurrent")), BooleanClause.Occur.SHOULD);
        }
        
        if (queryStr.contains("size") || queryStr.contains("empty") || queryStr.contains("isempty")) {
            booleanQuery.add(new TermQuery(new Term(field, "size")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "isempty")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "empty")), BooleanClause.Occur.SHOULD);
        }
        
        if (queryStr.contains("string") && (queryStr.contains("concat") || queryStr.contains("append"))) {
            booleanQuery.add(new TermQuery(new Term(field, "string")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "stringbuilder")), BooleanClause.Occur.SHOULD);
            booleanQuery.add(new TermQuery(new Term(field, "concatenation")), BooleanClause.Occur.SHOULD);
        }
    }

    /**
     * Converts a Lucene Document back to a KnowledgeEntry object.
     * 
     * @param doc Lucene document from search results
     * @return KnowledgeEntry object populated from document fields
     */
    private KnowledgeEntry documentToKnowledgeEntry(Document doc) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setTitle(doc.get("title"));
        entry.setType(doc.get("type"));
        entry.setDescription(doc.get("description"));
        entry.setExample(doc.get("example"));
        entry.setReference(doc.get("reference"));
        
        String tagsStr = doc.get("tags");
        if (tagsStr != null && !tagsStr.trim().isEmpty()) {
            entry.setTags(Arrays.asList(tagsStr.split(" ")));
        } else {
            entry.setTags(new ArrayList<>());
        }
        
        return entry;
    }
}