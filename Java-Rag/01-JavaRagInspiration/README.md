# Java RAG Code Review System

A Retrieval-Augmented Generation (RAG) system for Java code review that combines static analysis tools (Checkstyle and PMD) with a local knowledge base to provide contextual feedback.

## Project Structure

```
javaraginspiration/
├── pom.xml                                  # Maven build file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── epam/
│   │   │           ├── analysis/            # Static analysis components
│   │   │           │   ├── CheckstyleAnalyzer.java
│   │   │           │   └── PMDAnalyzer.java
│   │   │           ├── feedback/            # Feedback generation (RAG: Generation)
│   │   │           │   └── FeedbackGenerator.java
│   │   │           ├── model/               # Data models
│   │   │           │   ├── AnalysisFinding.java
│   │   │           │   └── KnowledgeEntry.java
│   │   │           ├── retrieval/           # RAG: Retrieval part
│   │   │           │   ├── KnowledgeBaseIndexer.java
│   │   │           │   └── KnowledgeBaseSearcher.java
│   │   │           └── Main.java            # Main entry point
│   │   └── resources/
│   │       ├── checkstyle.xml               # Checkstyle configuration
│   │       ├── pmd-ruleset.xml              # PMD ruleset configuration
│   │       ├── knowledgebase/               # Knowledge base entries
│   │       │   ├── vector_vs_arraylist.json
│   │       │   ├── enumeration_vs_iterator.json
│   │       │   └── synchronized_vs_concurrent.json
│   │       └── templates/
│   │           └── feedback_template.txt    # Feedback formatting template
└── TestClass.java                          # Sample file for testing
```

## Features

- **Static Analysis Integration**: Uses Checkstyle and PMD for comprehensive code analysis
- **RAG Pipeline**: Combines findings with knowledge base entries for contextual feedback
- **Intelligent Search**: Dynamic pattern matching that automatically learns from knowledge base entries
- **Extensible Knowledge Base**: JSON-based knowledge entries for easy maintenance
- **Template-based Feedback**: Customizable output formatting through templates
- **Configurable Rules**: Customizable Checkstyle and PMD rulesets

## Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build
```bash
cd javaraginspiration
mvn clean compile
```

### Run
```bash
mvn exec:java -Dexec.mainClass="com.epam.Main" -Dexec.args="TestClass.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index"
```

### Package as JAR
```bash
mvn clean package
java -jar target/javaraginspiration-1.0-SNAPSHOT.jar TestClass.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index
```

## Usage

```
java -jar javaraginspiration.jar <JavaFile> <CheckstyleConfig> <PMDRuleset> <KnowledgeBaseDir> <IndexDir>
```

**Arguments:**
- `JavaFile`: Path to a Java source file to analyze
- `CheckstyleConfig`: Path to a Checkstyle configuration XML file
- `PMDRuleset`: Path to PMD ruleset XML file  
- `KnowledgeBaseDir`: Directory containing JSON knowledge base files
- `IndexDir`: Directory for Lucene index (created automatically)

## Adding Knowledge Base Entries

Create JSON files in the `knowledgebase` directory with this structure:

```json
{
  "title": "Rule or Best Practice Title",
  "type": "Best Practice|Anti-pattern|Enhancement",
  "description": "Detailed explanation of the issue and why it matters",
  "example": "Code example showing the problem and solution",
  "reference": "Source or reference documentation",
  "tags": ["keyword1", "keyword2", "keyword3"]
}
```

## Customizing Rules

- **Checkstyle**: Modify `src/main/resources/checkstyle.xml`
- **PMD**: Modify `src/main/resources/pmd-ruleset.xml`

## How It Works

1. **Indexing**: Knowledge base entries are indexed using Apache Lucene with automatic pattern extraction
2. **Analysis**: Checkstyle and PMD analyze the Java code for issues
3. **Retrieval**: Intelligent search automatically matches findings with relevant knowledge entries
4. **Generation**: Template-based feedback generation combines findings with knowledge
5. **Output**: Formatted feedback is displayed to the user

## Example Output

```
=== CODE REVIEW FEEDBACK ===
Issue Detected: Vector
Location: TestClass.java:8 - Vector is legacy, prefer ArrayList
Knowledge Base Guidance:
Title: Vector is legacy, prefer ArrayList
Type: Anti-pattern
Description: The Vector class is considered legacy and should be replaced with ArrayList...
Example/Suggestion: Instead of: Vector<String> v = new Vector<>(); Use: ArrayList<String> list = new ArrayList<>();
Reference: Effective Java by Joshua Bloch, Item 6
=============================
```