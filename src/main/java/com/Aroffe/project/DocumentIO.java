package com.Aroffe.project;
import java.io.File;
import java.net.URI;
public abstract class DocumentIO
{
   protected File baseDir;
   public DocumentIO(File baseDir)
   {
      this.baseDir = baseDir;
   }
   /**
    * @param doc to serialize
    * @return File object representing file on disk to which document was serialized
    */
   public File serialize(Document doc)
   {
      return null;
   }

   public Document deserialize(URI uri)
   {
      return null;
   }
}