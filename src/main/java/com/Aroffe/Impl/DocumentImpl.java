package main.java.com.Aroffe.Impl;

import main.java.com.Aroffe.Document;
import main.java.com.Aroffe.DocumentStore;

import java.net.URI;
import java.util.Map;

public class DocumentImpl implements Document
{
   private byte[] byteArray;
   private int stringHashCode;
   private URI uri;
   private DocumentStore.CompressionFormat format;
   private Map<String, Integer> wordMap;
   private long time;

    /**
     *
     * @param byteArray, compressed byte array
     * @param stringHashCode, hashcode of decompressed String
     * @param uri, the key for the Document
     * @param format, the CompressionFormat specified, or default otherwise
     */
    DocumentImpl(byte[] byteArray, int stringHashCode, URI uri, DocumentStore.CompressionFormat format, Map<String, Integer> wordMap)
    {
        this.byteArray = byteArray;
        this.stringHashCode = stringHashCode;
        this.uri = uri;
        this.format = format;
        this.wordMap = wordMap;
        time = 0;
    }

    DocumentImpl(byte[] byteArray, int stringHashCode, URI uri, DocumentStore.CompressionFormat format)
    {
       this.byteArray = byteArray;
       this.stringHashCode = stringHashCode;
       this.uri = uri;
       this.format = format;
       time = 0;
    }

    public byte[] getDocument()
    {
        return byteArray;
    }

    public int getDocumentHashCode()
    {
        return stringHashCode;
    }

    public URI getKey()
    {
        return uri;
    }

    public DocumentStore.CompressionFormat getCompressionFormat()
    {
        return this.format;
    }
	
    public int wordCount(String word)
	{
		return wordMap.get(word);
	}

	 public long getLastUsedTime()
   {
      return this.time;
   }

    public void setLastUsedTime(long timeInMilliseconds)
   {
      this.time = timeInMilliseconds;
   }

   @Override
   public Map<String, Integer> getWordMap()
   {
      return wordMap;
   }

   @Override
   public void setWordMap(Map<String, Integer> wordMap)
   {
      this.wordMap = wordMap;
   }

   @Override
    public boolean equals(Object other)
    {
      if (this == other) return true;
      if (other == null || other.getClass() != getClass()) return false;
      Document doc = (Document) other;
      return this.getKey() == doc.getKey();
    }

   /**
    * The natural ordering of Documents is the order in which they were used
    * FIRST SUBTRACT - cast the result of the subtraction so each 'time' doesn't lose information
    * @param o, Document to compare to
    * @return positive int if the passed Document's last used time (in milliseconds) is less than this, negative int otherwise
    */
    @Override
    public int compareTo(Document o)
    {
      return ((int) (this.getLastUsedTime() -  o.getLastUsedTime()));
    }
}
