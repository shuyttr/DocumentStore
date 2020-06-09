# DocumentStore

DocumentStore is a Java library that mimics a simple search engine.

## Build
I created a pom.xml file that can easily be pulled and used. The code was compiled for Java 11.

## Usage

```java
import com.Aroffe.project;
Document doc = new Document(...);
DocumentStore.add(doc);
DocumentStore.search("keyword");
DocumentStore.delete(doc);
```

## Testing

I use the JUnit-4.12 test suite to test the DocumentStore's functionallity
