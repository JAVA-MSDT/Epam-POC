package com.epam.retrieval;

import com.epam.model.AnalysisFinding;
import com.epam.model.KnowledgeEntry;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Searches the indexed knowledge base for entries relevant to analysis findings.
 * Uses Lucene to perform fast text-based searches across knowledge entries.
 */
public class KnowledgeBaseSearcher {
    private final String indexDirPath;
    private final Map<String, Set<String>> patternCache = new HashMap<>();

    /**
     * Creates a new knowledge base searcher.
     * 
     * @param indexDirPath Path to the Lucene index directory
     */
    public KnowledgeBaseSearcher(String indexDirPath) {
        this.indexDirPath = indexDirPath;
    }

    /**
     * Searches for knowledge entries that match patterns in the provided source code.
     * 
     * @param sourceCode The Java source code to analyze
     * @param fileName The name of the file being analyzed
     * @return List of findings based on knowledge base patterns
     */
    public List<AnalysisFinding> searchInCode(String sourceCode, String fileName) throws Exception {
        List<AnalysisFinding> findings = new ArrayList<>();
        
        Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
        try (DirectoryReader reader = DirectoryReader.open(indexDir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            Query query = new MatchAllDocsQuery();
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String tags = doc.get("tags");
                String title = doc.get("title");
                
                if (tags != null) {
                    String[] tagArray = tags.split(" ");
                    for (String tag : tagArray) {
                        if (tag.length() > 3 && sourceCode.toLowerCase().contains(tag.toLowerCase())) {
                            // Find line number
                            int lineNum = findLineNumber(sourceCode, tag);
                            findings.add(new AnalysisFinding(
                                title,
                                fileName + ":" + lineNum + " - Knowledge base pattern '" + tag + "' detected in code."
                            ));
                            break; // Avoid multiple findings for the same entry
                        }
                    }
                }
            }
        }
        
        return findings;
    }

    private int findLineNumber(String sourceCode, String pattern) {
        String[] lines = sourceCode.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains(pattern.toLowerCase())) {
                return i + 1;
            }
        }
        return 1;
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

            // Debug: Print what we're searching for
            System.out.println("    Searching knowledge base for: '" + queryStr + "'");
            
            // Search across multiple fields for better matching
            Query query = buildMultiFieldQuery(queryStr);
            
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
     * @return Lucene Query object for searching
     */
    private Query buildMultiFieldQuery(String queryStr) {
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
            
            // Handle dynamic pattern matching
            addDynamicPatternQueries(booleanQuery, field, normalizedQuery);
        }
        
        return booleanQuery.build();
    }
    
    /**
     * Dynamically adds pattern-based queries by extracting keywords from knowledge base entries.
     */
    private void addDynamicPatternQueries(BooleanQuery.Builder booleanQuery, String field, String queryStr) {
        try {
            if (patternCache.isEmpty()) {
                buildPatternCache();
            }
            
            // Find matching patterns for the query
            for (Map.Entry<String, Set<String>> entry : patternCache.entrySet()) {
                String pattern = entry.getKey();
                Set<String> relatedTerms = entry.getValue();
                
                if (queryStr.contains(pattern)) {
                    for (String term : relatedTerms) {
                        booleanQuery.add(new TermQuery(new Term(field, term)), BooleanClause.Occur.SHOULD);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not build dynamic patterns: " + e.getMessage());
        }
    }
    
    /**
     * Builds a cache of patterns by extracting keywords from all knowledge base entries.
     */
    private void buildPatternCache() throws Exception {
        Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
        try (DirectoryReader reader = DirectoryReader.open(indexDir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            Query query = new MatchAllDocsQuery();
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                extractPatternsFromDocument(doc);
            }
        }
    }
    
    /**
     * Extracts searchable patterns from a knowledge base document.
     */
    private void extractPatternsFromDocument(Document doc) {
        String title = doc.get("title");
        String description = doc.get("description");
        String tags = doc.get("tags");
        
        Set<String> allTerms = new HashSet<>();
        
        // Extract terms from title, description, and tags
        if (title != null) {
            allTerms.addAll(extractKeywords(title));
        }
        if (description != null) {
            allTerms.addAll(extractKeywords(description));
        }
        if (tags != null) {
            allTerms.addAll(Arrays.asList(tags.split(" ")));
        }
        
        // Create bidirectional mappings between terms
        for (String term : allTerms) {
            patternCache.computeIfAbsent(term.toLowerCase(), k -> new HashSet<>()).addAll(
                allTerms.stream().map(String::toLowerCase).collect(Collectors.toSet())
            );
        }
    }
    
    /**
     * Extracts meaningful keywords from text.
     */
    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s,.-]+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !isStopWord(word))
                .collect(Collectors.toSet());
    }
    
    /**
     * Simple stop word filter.
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old", "see", "two", "who", "boy", "did", "she", "use", "way", "will", "with");
        return stopWords.contains(word);
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