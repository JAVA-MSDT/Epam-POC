package com.epam.retrieval;

import com.epam.model.KnowledgeEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;

/**
 * Indexes knowledge base entries using Apache Lucene for fast retrieval.
 * Processes JSON files containing knowledge entries and creates searchable index.
 */
public class KnowledgeBaseIndexer {

    /**
     * Indexes all JSON knowledge base files from a directory into a Lucene index.
     * 
     * @param kbDirPath Path to directory containing JSON knowledge base files
     * @param indexDirPath Path where Lucene index will be created/stored
     * @throws Exception If indexing fails due to I/O or parsing errors
     */
    public void indexKnowledgeBase(String kbDirPath, String indexDirPath) throws Exception {
        Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        
        try (IndexWriter writer = new IndexWriter(indexDir, config)) {
            ObjectMapper mapper = new ObjectMapper();
            File kbDir = new File(kbDirPath);
            
            // Process all JSON files in knowledge base directory
            File[] jsonFiles = kbDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    indexKnowledgeEntry(file, mapper, writer);
                }
            }
        }
    }

    /**
     * Indexes a single knowledge entry JSON file into the Lucene index.
     * 
     * @param file The JSON file containing knowledge entry
     * @param mapper Jackson ObjectMapper for JSON parsing
     * @param writer Lucene IndexWriter for adding documents
     * @throws Exception If file processing fails
     */
    private void indexKnowledgeEntry(File file, ObjectMapper mapper, IndexWriter writer) throws Exception {
        KnowledgeEntry entry = mapper.readValue(file, KnowledgeEntry.class);
        Document doc = new Document();
        
        // Index all fields as searchable text
        doc.add(new TextField("title", entry.getTitle(), Field.Store.YES));
        doc.add(new StringField("type", entry.getType(), Field.Store.YES));
        doc.add(new TextField("description", entry.getDescription(), Field.Store.YES));
        doc.add(new TextField("example", entry.getExample(), Field.Store.YES));
        doc.add(new StringField("reference", entry.getReference(), Field.Store.YES));
        doc.add(new TextField("tags", String.join(" ", entry.getTags()), Field.Store.YES));
        
        writer.addDocument(doc);
    }
}