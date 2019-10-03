package main.java.com.Aroffe;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class DocumentIOImplTest
{
   private File baseDir;
   private DocumentIO documentIO;
   private Document document;
   private URI one;

   public DocumentIOImplTest(){
      try{
         this.one = new URI("https://www.yu.edu/documents/doc43");
      }catch(URISyntaxException u){
         u.printStackTrace();
      }
      baseDir = new File(System.getProperty("user.dir"));
      documentIO = new DocumentIOImpl(baseDir);

      String textOne = "Well this is the finish line";
      document = new DocumentImpl(textOne.getBytes(), Math.abs(textOne.hashCode()), this.one, DocumentStore.CompressionFormat.JAR, this.makeMapping(textOne));
   }

   /**
    * This test demonstrates the logic I used to take a URI and break it down into a classpath.
    * Once broken down into a classpath, I appended the directories to the baseDir
    * After this, I create a file on disc (provided that file on disc does not exist)
    * Last I write the contents to disc using a FileWriter
    * In my implementation of DocumentIOImpl, I put this process to use using similar helper methods and a similar process
    */
   @Test
   public void createFile(){
      try
      {
         URI uri = new URI("http://www.yu.edu/documents/doc390");//given the complete uri
         String path = uri.getAuthority()  + this.disectURI(uri);//get the Path which is www.yu.edu\documents
         File file = new File(baseDir.getAbsolutePath() + File.separator + path);//make a File with the full class path and with the "URI Path"
         Files.createDirectories(file.toPath());//create the directories we didn't have before, ie not apart of the baseDir

         File theFile = new File(file.getAbsolutePath() + File.separator + "doc390.txt");//create a new File with the document as the last piece
         //wrap in a try because if the file is already there than we don't need to create a file
         if(!theFile.exists())Files.createFile(theFile.toPath());//create this file in the computer

         //write to the file -- in the real application this is the writing of the json
         FileWriter fileWriter = new FileWriter(theFile);
         fileWriter.write("HaYom sishi BShabbos");
         fileWriter.flush();
         fileWriter.close();

      }
      catch(Exception e){
         e.printStackTrace();
      }
   }

   /**
    * This test confirms that my serialize and deserialize methods for DocumentIOImpl work
    */
   @Test
   public void test2()
   {
      documentIO.serialize(document);
      documentIO.deserialize(document.getKey());
   }

   private String disectURI(URI uri)
   {
      String path = uri.getPath();

      String[] splitter = path.split("/");
      StringBuilder stringBuilder = new StringBuilder();
      for(int i = 0; i < splitter.length - 1; i++)
      {
         stringBuilder.append(splitter[i]).append(File.separator);
      }
      return stringBuilder.toString();

   }

   private Map<String, Integer> makeMapping(String string){
      HashMap<String, Integer> result = new HashMap<>();
      String[] arr = string.split("\\s");
      for(String s : arr)
      {
         result.merge(s, 1, (a, b) -> a + b);

      }
      return result;
   }
}