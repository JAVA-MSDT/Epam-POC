package com.example.test;

import java.util.*;

/**
 * Sample Java class specifically designed to trigger knowledge base matches.
 * Contains code patterns that match our knowledge base entries.
 */
public class KnowledgeBaseTestExample {
    
    // This will match "vector_vs_arraylist.json" knowledge entry
    private Vector<String> legacyVector = new Vector<>();
    
    // This will match "synchronized_vs_concurrent.json" knowledge entry  
    private List<String> syncList = Collections.synchronizedList(new ArrayList<>());
    private Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
    
    public void demonstrateKnowledgeBaseMatches() {
        // This will match "enumeration_vs_iterator.json" knowledge entry
        Enumeration<String> enumeration = legacyVector.elements();
        while (enumeration.hasMoreElements()) {
            String element = enumeration.nextElement();
            System.out.println(element);
        }
        
        // More Vector usage to trigger knowledge base
        Vector<Integer> numbers = new Vector<>();
        numbers.add(1);
        numbers.add(2);
        
        // Using synchronized collections - should match knowledge base
        syncList.add("item1");
        syncMap.put("key1", "value1");
        
        // Additional patterns that might trigger knowledge base matches
        if (legacyVector.size() == 0) {  // Should use isEmpty()
            System.out.println("Vector is empty");
        }

        // This pattern is NOT detected by current Checkstyle/PMD but is in KB via the new tag
        String test = "badCodePatternForTest";
    }
    
    // Method using Vector in parameter - more Vector usage
    public void processVector(Vector<String> data) {
        Enumeration<String> e = data.elements();
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
    }
}