package com.example.good;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Example of good Java code following best practices.
 */
public class GoodCodeExample {
    
    private final List<String> names = new ArrayList<>();
    private final Map<String, Integer> scores = new HashMap<>();
    
    public void demonstrateGoodPractices() {
        // Using Iterator instead of Enumeration
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            System.out.println(name);
        }
        
        // Using concurrent collections for thread safety
        List<String> threadSafeList = new CopyOnWriteArrayList<>();
        
        // Efficient string building
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            result.append("Item ").append(i).append(" ");
        }
        
        // Switch with default case
        int value = 5;
        switch (value) {
            case 1:
                System.out.println("One");
                break;
            case 2:
                System.out.println("Two");
                break;
            default:
                System.out.println("Other");
                break;
        }
        
        // Using isEmpty() instead of size() == 0
        if (names.isEmpty()) {
            System.out.println("No names");
        }
        
        // Proper exception handling
        try {
            Integer.parseInt("123");
        } catch (NumberFormatException ex) {
            System.err.println("Invalid number format: " + ex.getMessage());
        }
    }
}