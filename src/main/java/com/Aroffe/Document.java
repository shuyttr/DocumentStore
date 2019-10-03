package main.java.com.Aroffe;

import java.net.URI;
import java.util.Map;

public interface Document extends Comparable<Document>
{
    byte[] getDocument();
    int getDocumentHashCode();
    URI getKey();
    DocumentStore.CompressionFormat getCompressionFormat();
    /**
     * how many times does the given word appear in the document?
     * @param word
     * @return
     */
    int wordCount(String word);
    long getLastUsedTime();
    void setLastUsedTime(long timeInMilliseconds);
    Map<String, Integer> getWordMap();
    void setWordMap(Map<String, Integer> wordMap);
}
