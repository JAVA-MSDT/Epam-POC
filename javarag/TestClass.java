package com.example.test;

import java.util.*;

/**
 * Sample Java class with intentional code issues for testing the RAG system.
 */
public class TestClass {
    
    // Using legacy Vector instead of ArrayList
    private Vector<String> names = new Vector<>();
    
    // Missing final modifier for utility class
    public class UtilityHelper {
        public static void doSomething() {
            System.out.println("Utility method");
        }
    }
    
    public void demonstrateIssues() {
        // Using legacy Enumeration instead of Iterator
        Enumeration<String> e = names.elements();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            System.out.println(name);
        }
        
        // Using synchronized collections instead of concurrent ones
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Unnecessary string concatenation in loop
        String result = "";
        for (int i = 0; i < 10; i++) {
            result += "Item " + i;
        }
        
        // Missing default case in switch
        int value = 5;
        switch (value) {
            case 1:
                System.out.println("One");
                break;
            case 2:
                System.out.println("Two");
                break;
        }
    }
    
    // Method with too many parameters
    public void methodWithManyParams(String a, String b, String c, String d, 
                                   String e, String f, String g, String h) {
        // Implementation
    }
}