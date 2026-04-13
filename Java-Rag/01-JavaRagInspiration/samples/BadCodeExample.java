package com.example.test;

import java.util.*;

/**
 * Sample Java class with intentional code issues for testing the RAG system.
 * This class demonstrates various anti-patterns and code quality issues.
 */
public class BadCodeExample {
    
    // Using legacy Vector instead of ArrayList
    private Vector<String> names = new Vector<>();
    
    // Using legacy Hashtable instead of HashMap
    private Hashtable<String, Integer> scores = new Hashtable<>();
    
    // Missing final modifier for utility class
    public class UtilityHelper {
        public static void doSomething() {
            System.out.println("Utility method");
        }
    }
    
    // Method with too many parameters (violates parameter count rules)
    public void methodWithManyParams(String a, String b, String c, String d, 
                                   String e, String f, String g, String h, String i) {
        // Implementation
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
        Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
        
        // Inefficient string concatenation in loop
        String result = "";
        for (int i = 0; i < 10; i++) {
            result += "Item " + i + " ";
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
        
        // Empty catch block
        try {
            Integer.parseInt("invalid");
        } catch (NumberFormatException ex) {
            // Empty catch block - bad practice
        }
        
        // Unnecessary wrapper object creation
        Boolean flag = new Boolean(true);
        Integer count = new Integer(42);
        
        // Using size() == 0 instead of isEmpty()
        if (names.size() == 0) {
            System.out.println("No names");
        }
    }
    
    // Method that doesn't follow naming conventions
    public void Method_With_Bad_Name() {
        // Bad method name
    }
    
    // Unused private method
    private void unusedMethod() {
        System.out.println("This method is never called");
    }
}