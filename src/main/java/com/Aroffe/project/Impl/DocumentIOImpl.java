package com.Aroffe.project.Impl;

import com.Aroffe.project.Document;
import com.Aroffe.project.DocumentIO;
import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;

/**
 * You do not serialize the lasUseTime. You must serialize/deserialize:
 *    1. the byte[] which has the contents of the document
 *    2. the compression format
 *    3. the URI/key
 *    4. the document hashcode
 *    5. the wordcount map.
 *    (Requirement Doc - 3.1)
 */
public class DocumentIOImpl extends DocumentIO
{

   /**
    * My DocumentStoreImpl will always pass a "baseDir" into DocumentIOImpl
    * Either one was passed when Constructing a DocumentStoreImpl, or it will be assigned
    * to the System property user.dir
    * @param baseDir, File to serialize the possible Files to
    */
   DocumentIOImpl(File baseDir)
   {
      super(baseDir);
   }

   /**
    * I used the following resources to understand
    * the dynamics of GSON and Serialization for this stage:
    * <I>
    * https://stackoverflow.com/questions/16239130/java-user-dir-property-what-exactly-does-it-mean
    * https://www.youtube.com/watch?v=cFCgFlqF5kw
    * http://www.javacreed.com/gson-serialiser-example/
    * http://www.studytrails.com/java/json/java-google-json-custom-serializer-deserializer/
    * </I>
    *
    * @param doc to serialize
    * @return a serialized <B>File</B> in JSON format
    */
   @Override
   public File serialize(Document doc) {

      try{
         Gson gson = new GsonBuilder()
              .registerTypeAdapter(DocumentImpl.class, new DocumentSerializer())
              .setPrettyPrinting()
              .create();
         //"Documents must be written to disk as JSON documents."
         String json = gson.toJson(doc);

         String path = doc.getKey().getAuthority() + this.disectURI(doc.getKey());
         File file = new File(this.baseDir.getAbsolutePath() + File.separator + path);
         Files.createDirectories(file.toPath());

         File theFile = new File(file.getAbsolutePath() + File.separator + this.saveTheDocument(doc.getKey()));
         if (!theFile.exists()) Files.createFile(theFile.toPath());


         FileWriter fileWriter = new FileWriter(theFile);
         fileWriter.write(json);
         fileWriter.flush();
         fileWriter.close();

         return file;
      } catch(IOException e) {
         e.printStackTrace();
         return null;
      }
   }


   /**
    * https://www.mkyong.com/java/how-do-convert-java-object-to-from-json-format-gson-api/
    * @param uri this uri represents where the item is saved on disc
    * @return Document that was previously a stored in a File
    */
   @Override
   public Document deserialize(URI uri) {
      try{
      Gson gsonDeserializer = new GsonBuilder()
              .registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer())
              .setPrettyPrinting()
              .create();
         String path = baseDir.getAbsolutePath() + File.separator + uri.getAuthority() + this.disectURI(uri) + this.saveTheDocument(uri);
         Reader reader = new FileReader(path);
         return gsonDeserializer.fromJson(reader, DocumentImpl.class);
      }
      catch (IOException e) {
         e.printStackTrace();
         return null;
      }

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

   private String saveTheDocument(URI uri){
      String path = uri.getPath();

      char[] pathChars = path.toCharArray();
      for(int i = 0; i < pathChars.length; i++)
      {
         if(pathChars[i] == 47 || pathChars[i] == 92)
         {
            pathChars[i] = File.separatorChar;
         }
      }

      String[] splitter = path.split("/");
      return splitter[splitter.length - 1] + ".json";
   }

}
