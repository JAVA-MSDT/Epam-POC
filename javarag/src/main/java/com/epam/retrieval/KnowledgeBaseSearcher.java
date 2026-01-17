package com.epam.retrieval;

import com.epam.model.KnowledgeEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
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

            // Search across multiple fields for better matching
            Query query = buildMultiFieldQuery(queryStr, analyzer);
            
            TopDocs topDocs = searcher.search(query, maxResults);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                results.add(documentToKnowledgeEntry(doc));
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
     * 
     * @param queryStr The search query string
     * @param analyzer Lucene analyzer for query processing
     * @return Lucene Query object for searching
     * @throws Exception If query parsing fails
     */
    private Query buildMultiFieldQuery(String queryStr, Analyzer analyzer) throws Exception {
        String[] fields = {"title", "description", "tags"};
        QueryParser parser = new QueryParser("tags", analyzer);
        
        // Escape special characters and create a query
        String escapedQuery = QueryParser.escape(queryStr);
        return parser.parse(escapedQuery);
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