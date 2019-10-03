package main.java.com.Aroffe;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class DocumentStoreImplTest
{
   private final ByteArrayInputStream stream5;
   private URI fifthID;
   private DocumentStore documentStore;
   private URI firstID, secondID, thirdID, fourthID;
   private InputStream stream1;
   private int byteTotal;

   /**
    * This test is merely a constructor that makes sure everything compiles and can be added to the DocStore without issue
    * @throws InterruptedException, not sure...
    */
   public DocumentStoreImplTest() throws InterruptedException
   {
      byteTotal = 0;
      documentStore = new DocumentStoreImpl();
      try
      {
         firstID = new URI("https://www.facebook.com/documents/doc1");
         secondID = new URI("https://www.twitter.com/documents/doc2");
         thirdID = new URI("https://www.yu.edu/documents/doc3");
         fourthID = new URI("https://www.google.com/documents/doc4");

         fifthID = new URI("https://www.yu.edu/documents/doc5");
      } catch(URISyntaxException e)
      {
         e.printStackTrace();
      }
      String string1 = "Hello my name hell is Bart Simpson";

      String string2 = "hello my name is bart simpson. hello world!";
      String string3 = "Welcome to Punctu'ation Lan4d, hell whe]re you'll find many ty9o,s";
      String string4 = "CaMeL CaSe, is he, isn't bart n33d3d hel for bart class's";

      String string5 = "this is the only new string for stage four. It is here for to stay. perhaps for stage five!";

      stream1 = new ByteArrayInputStream(string1.getBytes());
      InputStream stream2 = new ByteArrayInputStream(string2.getBytes());
      InputStream stream3 = new ByteArrayInputStream(string3.getBytes());
      InputStream stream4 = new ByteArrayInputStream(string4.getBytes());

      stream5 = new ByteArrayInputStream(string5.getBytes());

      documentStore.setDefaultCompressionFormat(DocumentStore.CompressionFormat.ZIP);
      documentStore.putDocument(stream1, firstID);
      Thread.sleep(50);
      documentStore.putDocument(stream2, secondID, DocumentStore.CompressionFormat.GZIP);
      Thread.sleep(50);
      documentStore.putDocument(stream3, thirdID, DocumentStore.CompressionFormat.BZIP2);
      Thread.sleep(50);
      documentStore.putDocument(stream4, fourthID, DocumentStore.CompressionFormat.SEVENZ);
      Thread.sleep(50);

      Map<String, Integer> map = new HashMap<>();
      Map<String, Integer> map2 = new HashMap<>();
      int i = 1;
      for(String element : string1.split("//s"))
      {
         map.put(element, i);
         map2.put(element, i);
         i++;
      }

   }

   /**
    * I heavily used the debugging tool - by stepping through each line- in order to see what has shipped to disc and what remains in memory
    * In this test I set the DocStore to have a maximum amount of Documents
    * Then I add one more Document and the least recently used, in this case "http://www.facebook.com/documents/doc1" will be sent to disc (as it is)
    * Next I add up all the bytes of the remaining Documents and change the DocStore doc limit to hold more than 4 documents
    * I then try to put the document that was sent to disc (key="https://www.facebook.com/documents/doc1" back into docStore
    * This indeed comes "back into memory" and effectively kicks out the next least recently used document which is "www.yu.edu/documents/doc5" which it does
    * Last, I delete the "facebook" document
    * @throws InterruptedException
    */
   @Test
   public void test3() throws InterruptedException
   {
      documentStore.setMaxDocumentCount(4);
      Thread.sleep(50);
      documentStore.putDocument(stream5, fifthID);
      Thread.sleep(50);

      int a = documentStore.getCompressedDocument(secondID).length;
      int b = documentStore.getCompressedDocument(thirdID).length;
      int c = documentStore.getCompressedDocument(fourthID).length;
      int d = documentStore.getCompressedDocument(fifthID).length;

      byteTotal = a + b + c + d;
      documentStore.setMaxDocumentCount(10);
      documentStore.setMaxDocumentBytes(byteTotal);

      documentStore.putDocument(stream1, firstID);

      documentStore.deleteDocument(firstID);

      //todo test undo for delete
   }
}
