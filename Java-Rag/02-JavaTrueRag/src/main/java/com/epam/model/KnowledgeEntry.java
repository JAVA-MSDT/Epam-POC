package com.epam.model;

import java.util.List;

/**
 * Represents a knowledge base entry containing best practices, antipatterns, or coding guidelines.
 * Used in the RAG system to provide contextual feedback for static analysis findings.
 */
public class KnowledgeEntry {
    private String title;
    private String type;
    private String description;
    private String example;
    private String reference;
    private List<String> tags;

    /**
     * Gets the title of the knowledge entry.
     * 
     * @return The entry title
     */
    public String getTitle() { 
        return title; 
    }

    /**
     * Sets the title of the knowledge entry.
     * 
     * @param title The entry title
     */
    public void setTitle(String title) { 
        this.title = title; 
    }

    /**
     * Gets the type of the knowledge entry (e.g., "Best Practice", "Anti-pattern").
     * 
     * @return The entry type
     */
    public String getType() { 
        return type; 
    }

    /**
     * Sets the type of the knowledge entry.
     * 
     * @param type The entry type
     */
    public void setType(String type) { 
        this.type = type; 
    }

    /**
     * Gets the detailed description of the knowledge entry.
     * 
     * @return The entry description
     */
    public String getDescription() { 
        return description; 
    }

    /**
     * Sets the detailed description of the knowledge entry.
     * 
     * @param description The entry description
     */
    public void setDescription(String description) { 
        this.description = description; 
    }

    /**
     * Gets the example code or usage for the knowledge entry.
     * 
     * @return The entry example
     */
    public String getExample() { 
        return example; 
    }

    /**
     * Sets the example code or usage for the knowledge entry.
     * 
     * @param example The entry example
     */
    public void setExample(String example) { 
        this.example = example; 
    }

    /**
     * Gets the reference or source for the knowledge entry.
     * 
     * @return The entry reference
     */
    public String getReference() { 
        return reference; 
    }

    /**
     * Sets the reference or source for the knowledge entry.
     * 
     * @param reference The entry reference
     */
    public void setReference(String reference) { 
        this.reference = reference; 
    }

    /**
     * Gets the tags associated with the knowledge entry for search purposes.
     * 
     * @return The entry tags
     */
    public List<String> getTags() { 
        return tags; 
    }

    /**
     * Sets the tags associated with the knowledge entry.
     * 
     * @param tags The entry tags
     */
    public void setTags(List<String> tags) { 
        this.tags = tags; 
    }
}