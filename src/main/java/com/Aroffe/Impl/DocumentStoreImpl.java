package main.java.com.Aroffe.Impl;

import edu.yu.cs.com1320.project.*;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

import static java.nio.file.Files.readAllBytes;

/**
 * @version 5.9.19
 */

public class DocumentStoreImpl implements DocumentStore
{
   private BTreeImpl<URI, Document> bTree;
   private CompressionFormat format;
   private Stack<Command> commandStack;
   private Trie<URI> trieOfLists;
   private WordComparator comparator;
   private int documentLimit;
   private int byteLimit;
   private int currentDocSize;
   private int currentByteSize;
   private MinHeapImpl<URI> minHeap;
   /**
    * Constructor with a File provided
    */
   public DocumentStoreImpl(File baseDir)
   {
      bTree = new BTreeImpl<>(baseDir);
      format = CompressionFormat.ZIP;
      commandStack = new StackImpl<>();
      comparator = new WordComparator(bTree);
      trieOfLists = new TrieImpl<>(comparator);
      this.documentLimit = Integer.MAX_VALUE;
      byteLimit = Integer.MAX_VALUE;
      currentDocSize = 0;
      currentByteSize = 0;
      minHeap = new MinHeapImpl<>(bTree);
   }

   /**
    * Default Constructor without a File to Serialize document to -- ie without a base directory
    */
   public DocumentStoreImpl()
   {
      bTree = new BTreeImpl<>(new File(System.getProperty("user.dir")));//
      this.format = CompressionFormat.ZIP;
      commandStack = new StackImpl<>();
      comparator = new WordComparator(bTree);
      trieOfLists = new TrieImpl<>(comparator);
      this.documentLimit = Integer.MAX_VALUE;
      this.byteLimit = Integer.MAX_VALUE;
      currentDocSize = 0;
      currentByteSize = 0;
      minHeap = new MinHeapImpl<>(bTree);
   }

   /**
    * This method returns the Strings in decreasing order by the amount of times the word appears in that Document
    * @param keyword, the word that we are searching
    * @return List<String> whereby each String is an uncompressed Document that holds keyword
    */
   @Override
   public List<String> search(String keyword)
   {
      comparator.setKey(keyword);
      List<URI> docList = trieOfLists.getAllSorted(keyword);
      if (docList == null) return new ArrayList<>();
      List<String> result = new ArrayList<>();

      long time = System.currentTimeMillis();//all documents iterated should have the same time stamp
      for(URI element : docList)
      {
         Document doc = (Document) bTree.get(element);
         result.add(this.getDocument(doc.getKey()));
         doc.setLastUsedTime(time); //update the last used time
         minHeap.reHeapify(element); //move the document in the heap accordingly
      }

      return result;
   }

   /**
    * This method returns the byte[] in decreasing order by the amount of times the word appears in that Document
    * @param keyword, the word that we are searching
    * @return List<byte[]> whereby each byte[] is compressed data of the Documents that hold keyword
    */
   @Override
   public List<byte[]> searchCompressed(String keyword)
   {
      comparator.setKey(keyword);
      List<URI> documentList = trieOfLists.getAllSorted(keyword);
      if (documentList == null) return new ArrayList<>();
      List<byte[]> result = new ArrayList<>();

      long time = System.currentTimeMillis();
      for(URI element : documentList)
      {
         Document doc = (Document) bTree.get(element);
         result.add(doc.getDocument());
         doc.setLastUsedTime(time); //see line 89
         minHeap.reHeapify(element);//see line 90
      }
      return result;
   }

   /**
    * User supplied format
    * @param format, Sets the default compression
    */
   public void setDefaultCompressionFormat(CompressionFormat format)
   {
      this.format = format;
   }

   /**
    * @return the default CompressionFormat
    */
   @Override
   public CompressionFormat getDefaultCompressionFormat()
   {
      return this.format;
   }

   /**
    * This covers cases of overriding an existing Document:
    * 1. save a copy of String of Document and a reference to the Document
    * 2. put the new Document in
    * 3. delete the previous entry from the Trie, put in new words
    *
    * "I am not going to insist that docs pushed to disk via a put are brought back into memory via undo."
    * -->> therefore I do not handle undo on a put that overid a document
    * @param input the document being put
    * @param uri   unique identifier for the document
    * @return the Documents hash code
    */
   public int putDocument(InputStream input, URI uri)
   {
      Document oldOneIfItExists = (Document) bTree.get(uri);//every time bTree.get() would return a File, that file is deleted and itsDocument is brought into memory
      URI oldURI = null;
      if (oldOneIfItExists != null ) {
         oldURI = oldOneIfItExists.getKey();
      }
      String oldDocumentString = this.getDocument(oldURI); //if not in table then oldDocumentString == null
      this.deleteTrie(oldDocumentString, oldURI);//first delete the word in that Trie

      Document freshDoc = this.ultimatePut(input, uri, this.getDefaultCompressionFormat());

      Function<URI, Boolean> undo = (URI dif) -> {
         bTree.put(dif, null); //undo on a put is a delete
         this.deleteTrie(this.getDePuncuatedString(this.getDocument(((Document)bTree.get(dif)).getKey())), uri); //take out most recent Document from the Trie
         return true;
      };
      Function<URI, Boolean> redo = (URI dif) -> {
         bTree.put(dif, freshDoc); //put back the document we wanted to put in originally
         try{
            this.putIntoTrie(this.getDePuncuatedString(this.getDocument(freshDoc.getKey())), freshDoc);
         }catch (NullPointerException n){
            n.printStackTrace();
         }
         return true;
      };
      commandStack.push(new Command(uri, undo, redo));
      if(bTree.get(uri) != null) return ((Document) bTree.get(uri)).getDocumentHashCode();
      else return Integer.MIN_VALUE; //see piazza posting 3.20.19
   }

   private void deleteTrie(String s, URI uri)
   {
      if (s == null) return;
      s = this.getDePuncuatedString(s);
      String[] tokens = s.split("\\s");
      for(String element : tokens){
         comparator.setKey(element);
         if (trieOfLists.getAllSorted(element) == null || trieOfLists.getAllSorted(element).size() == 1){
            trieOfLists.deleteAll(element);
            continue;
         }
         trieOfLists.delete(element, uri);//fix this line to work with URI update
      }
   }

   private byte[] getByteArray(byte[] byteArr, DocumentStore.CompressionFormat format)
   {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      byte[] result;
      switch(format)
      {
         case JAR:
            result = this.jarCompression(bos, byteArr);
            break;
         case GZIP:
            result = this.gzipCompression(bos, byteArr);
            break;
         case BZIP2:
            result = this.bzip2Compression(bos, byteArr);
            break;
         case SEVENZ:
            result = this.sevenzCompression(byteArr);
            break;
         default:
            result = this.zipCompressed(bos, byteArr);
      }
      return result;
   }

   private byte[] sevenzCompression(byte[] byteArr)
   {
      File file = null;
      try
      {
         file = File.createTempFile("this", "file");
         file.deleteOnExit();
         SevenZOutputFile sevenZOutputFile = new SevenZOutputFile(file);
         SevenZArchiveEntry entry = sevenZOutputFile.createArchiveEntry(file, "taka");
         sevenZOutputFile.putArchiveEntry(entry);
         sevenZOutputFile.write(byteArr);
         sevenZOutputFile.closeArchiveEntry();
         sevenZOutputFile.close();
      } catch(Exception e)
      {
         e.printStackTrace();
      }
      byte[] sevenzCompressed = new byte[0];

      try
      {
         assert file != null;
         sevenzCompressed = readAllBytes(file.toPath());
      } catch(IOException e)
      {
         e.printStackTrace();
      }
      return sevenzCompressed;

   }

   private byte[] bzip2Compression(ByteArrayOutputStream bos, byte[] byteArr)
   {
      try
      {
         BZip2CompressorOutputStream bzipout = new BZip2CompressorOutputStream(bos);
         bzipout.write(byteArr);
         bzipout.close();
         return bos.toByteArray();
      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
   }

   private byte[] zipCompressed(ByteArrayOutputStream bos, byte[] byteArr)
   {
      try
      {
         ZipArchiveOutputStream zipout = new ZipArchiveOutputStream(bos);
         ZipArchiveEntry a = new ZipArchiveEntry("name");
         zipout.putArchiveEntry(a);
         zipout.write(byteArr);
         zipout.closeArchiveEntry();
         zipout.flush();
         zipout.close();
      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }

      return bos.toByteArray();
   }

   private byte[] gzipCompression(ByteArrayOutputStream bos, byte[] byteArr)
   {
      try
      {
         GzipCompressorOutputStream gzout = new GzipCompressorOutputStream(bos);
         gzout.write(byteArr);
         gzout.close();
         return bos.toByteArray();
      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
   }

   private byte[] jarCompression(ByteArrayOutputStream bos, byte[] byteArr)
   {
      try
      {
         JarArchiveOutputStream jarout = new JarArchiveOutputStream(bos);
         JarArchiveEntry entry = new JarArchiveEntry("name");
         jarout.putArchiveEntry(entry);
         jarout.write(byteArr);
         jarout.closeArchiveEntry();
         jarout.flush();
         jarout.close();

      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
      return bos.toByteArray();
   }

   /**
    * @param input  the document being put
    * @param uri    unique identifier for the document
    * @param format compression format to use for compressing this document
    * @return the Document hashcode
    */
   public int putDocument(InputStream input, URI uri, CompressionFormat format) {
      Document ifOldOneExists = (Document) bTree.get(uri); URI oldOne = null;
      if (ifOldOneExists != null){
         oldOne = ifOldOneExists.getKey();
      }
      String oldDocumentString = this.getDocument(oldOne);
      String result = this.getDePuncuatedString(oldDocumentString);
      this.deleteTrie(oldDocumentString, oldOne);
      this.ultimatePut(input, uri, this.getDefaultCompressionFormat());

      /*
         docs pushed to disk via a put are brought back into memory via undo
         therefore, my undo for puts merely deletes the existing Document and keeps no record of a "previous" document
       */
      Function<URI, Boolean> undo = (URI dif) -> {
         bTree.put(dif, null);//deletes it - do not support reputing docs from disc
         this.deleteTrie(this.getDePuncuatedString(this.getDocument(((Document) bTree.get(dif)).getKey())), uri); //delete the "new" stuff -> vadai there
         return true;
      };
      Function<URI, Boolean> redo = (URI dif) -> {
         bTree.put(dif, bTree.get(dif));
         if(result != null) this.deleteTrie(result, uri);
         trieOfLists.put(this.getDePuncuatedString(this.getDocument(((Document)bTree.get(dif)).getKey())), dif);
         minHeap.insert(uri);
         minHeap.reHeapify(uri);
         return true;
      };
      commandStack.push(new Command(uri, undo, redo));
      if(bTree.get(uri) != null) return ((Document) bTree.get(uri)).getDocumentHashCode();
      else return Integer.MIN_VALUE;
   }

   private Document ultimatePut(InputStream input, URI uri, CompressionFormat format)
   {
      try
      {
         byte[] byteArr = IOUtils.toByteArray(input);
         String doc = new String(byteArr);
         String punctuationLessString = this.getDePuncuatedString(doc); //get the String without any punctuation, and all lower case
         HashMap<String, Integer> map = this.getMaping(punctuationLessString);
         int hash = Math.abs(doc.hashCode());//string "ab" and "ba" have same hashCode so 'if' statement below won't handle this
         Document freshDoc;

         byte[] result = this.getByteArray(byteArr, format);
         //if no doc previously with that uri, or doc previously but now its a 'different' string
         if((bTree.get(uri) == null) || (hash != ((Document) bTree.get(uri)).getDocumentHashCode()))
         {
            freshDoc = new DocumentImpl(result, hash, uri, format, map);
            this.handleOverflow(freshDoc, uri);
            this.putIntoTrie(punctuationLessString, freshDoc);
         }
         return (Document) bTree.get(uri);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
   }

   private void handleOverflow(Document freshDoc, URI uri)
   {
      int byteCapacity = freshDoc.getDocument().length;//amount of bytes in the (byte array) Document
      if (byteCapacity > byteLimit) throw new IllegalArgumentException();//Document size must be less than DocumentStore byte limit
      currentDocSize++; currentByteSize += byteCapacity; //this line includes the bytes of the doc we want to put in
      //while this doc does overflow the doc limit or the byte limit, take away the min
      while(this.currentDocSize > this.documentLimit || this.currentByteSize > this.byteLimit)
      {
         //When a document is written out to disk, it is removed from the MinHeap which is managing memory (Requirements Doc-2.1)
         URI toRemove = minHeap.removeMin();
         Document docToRemoveAllTraces = (Document) bTree.get(toRemove);
         //keep removing the least recently used Document - represented as URI - til you get to a document that's still in the table
         //that is, a call to getKey is non null -> if it is null, been deleted by DocStore.delete() then take out 'next min', hence continue
         if(bTree.get(toRemove) == null) {
            continue;
         }
         this.deleteTrie(this.getDocument(docToRemoveAllTraces.getKey()), toRemove);//remove all its words from the Trie
         try{
            /*
            When a document has to be kicked out of memory, instead of it being deleted completely
            it will be written to disk via a call to BTree.moveToDisk.
            When a document is moved to disk, the entry in the BTree has a reference to the
            file on disk instead of a reference to the document in memory (Requirements Doc-2)
             */
            bTree.moveToDisk(toRemove);
         } catch (Exception e) {
            e.printStackTrace();
         }
         currentDocSize--;
         currentByteSize -= byteCapacity;
      }
      //now we have taken out the document(s) we needed to take out to make room for this one, so insert it
      freshDoc.setLastUsedTime(System.currentTimeMillis());//"it is being used" so set its time stamp
      minHeap.insert(freshDoc.getKey());//inserts at last place in the heap so no need to reHeapify
      bTree.put(uri, freshDoc);//put the new Document into the docStore
   }

   private void putIntoTrie(String punctuationLessString, Document freshDoc)
   {
      if (punctuationLessString == null) return;
      String[] tokens = punctuationLessString.split("\\s");
      for(String token : tokens)
      {
         trieOfLists.put(token, freshDoc.getKey());
      }
   }

   private String getDePuncuatedString(String input)
   {
      if (input == null) return null;
      int takeAway = 0;
      char[] charArray;
      charArray = input.toCharArray();
      for(char c : charArray)
      {
         if(!(Character.isLetter(c) || Character.isWhitespace(c)))
         {
            takeAway++;
         }
      }

      char[] updatedChars = new char[charArray.length - takeAway];
      int j = 0;
      for(char c : charArray)
      {
         if(Character.isLetter(c) || Character.isWhitespace(c))
         {
            updatedChars[j] = c;
            j++;
         }
      }
      String dif = new String(updatedChars);
      return dif.toLowerCase();
   }

   private HashMap<String, Integer> getMaping(String uncompressedDocument)
   {
      HashMap<String, Integer> result = new HashMap<>();

      String[] tokensOfStrings;
      try
      {
         tokensOfStrings = uncompressedDocument.split("\\s");
         for(String tokensOfString : tokensOfStrings)
         {
            result.merge(tokensOfString, 1, (a, b) -> a + b);
         }
      } catch(PatternSyntaxException p)
      {
         p.printStackTrace();
      }
      return result;
   }


   /**
    * "whenever any other part of the code needs a doc, it calls btree.get().
    * If btree has the doc already in memory,it returns it. If the doc is on disk,
    * it deserializes the doc and then returns it."
    * -instructor answer to E. Perl's Piazza question (5/29/2019)
    *
    * @param uri the unique identifier of the document to get
    * @return the <I><B>Decompressed</B></I> Document String
    */
   public String getDocument(URI uri)
   {
      Document lookup = (Document) bTree.get(uri);//brings the Document into memory (Requirement Doc-2.2)
      if (lookup == null) return null;
      int bytes = lookup.getDocument().length;
      int amountOfDocs = minHeap.size(); //amount of docs is whatever is in the minHeap, ie it is in memory
      while(currentByteSize + bytes > byteLimit || amountOfDocs > documentLimit) {
         URI toRemove = minHeap.removeMin();
         Document doc = (Document) bTree.get(toRemove);
         currentByteSize-= doc.getDocument().length;
         amountOfDocs--;
         currentDocSize--;
         bTree.moveToDisk(toRemove);//no URI's that represent "Files" in the minHeap, just Documents
      }
      //bTree.put(uri, lookup);//put it "in-memory"
      lookup.setLastUsedTime(System.currentTimeMillis());
      DocumentStore.CompressionFormat lala = lookup.getCompressionFormat();
      byte[] compressedBytes = lookup.getDocument();
      byte[] result;
      switch(lala) {
         case GZIP:
            result = this.decompressGZIP(compressedBytes);
            break;
         case BZIP2:
            result = this.decompressBZIP2(compressedBytes);
            break;
         case JAR:
            result = this.decompressJAR(compressedBytes);
            break;
         case SEVENZ:
            result = this.decompressSevenZ(compressedBytes);
            break;
         case ZIP:
         default:
            result = this.decompressZIP(compressedBytes);
      }
      if(minHeap.getArrayIndex(lookup.getKey()) != 0){
         minHeap.reHeapify(lookup.getKey());
      }
      if (result == null) return null;
      return new String(result);
   }

   private byte[] decompressGZIP(byte[] in)
   {
      try
      {
         ByteArrayInputStream bis = new ByteArrayInputStream(in);
         GzipCompressorInputStream gzipin = new GzipCompressorInputStream(bis);
         return gzipin.readAllBytes();
      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
   }

   private byte[] decompressBZIP2(byte[] in)
   {
      try
      {
         ByteArrayInputStream bis = new ByteArrayInputStream(in);
         BZip2CompressorInputStream bzipin = new BZip2CompressorInputStream(bis);
         return bzipin.readAllBytes();
      } catch(IOException e)
      {
         e.printStackTrace();
         return null;
      }

   }

   private byte[] decompressJAR(byte[] in)
   {
      ByteArrayInputStream bis = new ByteArrayInputStream(in);
      JarArchiveInputStream jarin = new JarArchiveInputStream(bis);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      JarArchiveEntry entry;
      try
      {
         entry = (JarArchiveEntry) jarin.getNextEntry();
         bos.write(jarin.readAllBytes());
         bos.close();
         jarin.close();
         bis.close();
      } catch(Exception e)
      {
         e.printStackTrace();
         return null;
      }
      return bos.toByteArray();
   }

   private byte[] decompressZIP(byte[] in)
   {
      ByteArrayInputStream bis = new ByteArrayInputStream(in);
      ZipArchiveInputStream zipin = new ZipArchiveInputStream(bis);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ZipArchiveEntry entry;
      try
      {
         entry = (ZipArchiveEntry) zipin.getNextEntry();
         bos.write(zipin.readAllBytes());
         bos.close();
         zipin.close();
         bis.close();
         return bos.toByteArray();
      } catch(IOException e)
      {
         e.printStackTrace();
         return null;
      }
   }

   private byte[] decompressSevenZ(byte[] in)
   {
      File file;
      byte[] bytes = null;
      try
      {
         file = File.createTempFile("well", "there");
         FileOutputStream fileOutputStream = new FileOutputStream(file);
         fileOutputStream.write(in);
         fileOutputStream.close();
         file.deleteOnExit();

         SevenZFile sevenZFile = new SevenZFile(file);
         SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getNextEntry();
         bytes = new byte[(int) sevenZArchiveEntry.getSize()];

         sevenZFile.read(bytes);

      } catch(IOException e)
      {
         e.printStackTrace();
      }
      return bytes;
   }

   /**
    * @param uri the unique identifier of the document to get
    * @return the byte array of compressed Document object
    */
   public byte[] getCompressedDocument(URI uri)
   {
      Document found = (Document) bTree.get(uri);
      int bytes = found.getDocument().length;
      while(currentByteSize + bytes > byteLimit || currentDocSize > documentLimit){
         URI toRemove = minHeap.removeMin();
         Document document = (Document) bTree.get(toRemove);
         currentByteSize-= document.getDocument().length;
         currentDocSize--;
         if (bTree.get(toRemove) == null) continue;//this line indicates that it was deleted
         bTree.moveToDisk(toRemove);
         }

      found.setLastUsedTime(System.currentTimeMillis());
      minHeap.reHeapify(uri);
      return found.getDocument();
   }

   /**
    * Strategy
    * 1. take out this Document from the Trie
    * 2. then take out the Document from our HashTable
    *
    * @param uri the unique identifier of the document to delete
    * @return true if the document exists, false if the given uri has no document to it
    */
   public boolean deleteDocument(URI uri)
   {
      Document ahh = (Document) bTree.get(uri);
      if(ahh == null) return false; //no Document exists for the provided URI

      String string = this.getDePuncuatedString(this.getDocument(uri));
      this.deleteTrie(string,uri);

      bTree.put(uri, null);

      Function<URI, Boolean> undo = (URI some) -> {
         this.handleOverflow(ahh, ahh.getKey());
         bTree.put(some, ahh);
         this.putIntoTrie(string, ahh);
         return true;
      };

      Function<URI, Boolean> redo = (URI some) -> {
         bTree.put(some, null);
         this.deleteTrie(string, uri);
         return true;
      };
      commandStack.push(new Command(uri, undo, redo));
      return true;
   }

   /**
    * @return true if there is at least one command on the stack to undo, false if stack is empty
    * @throws IllegalStateException if nothing is on stack throw exception and return false
    */
   public boolean undo() throws IllegalStateException
   {
      try {
         Command undoThisCommand = commandStack.pop();
         if(undoThisCommand == null) {
            throw new IllegalStateException();
         }

         undoThisCommand.undo();
         return true;

      } catch(IllegalStateException a)
      {
         a.printStackTrace();
         return false;
      }
   }

   /**
    * @param uri the Document to delete, identified by its uri
    * @return true if there exists a Command in the stack with the given URI, false otherwise
    * @throws IllegalStateException if Document store doesn't have a Document with the given uri, or Stack is empty
    */
   public boolean undo(URI uri) throws IllegalStateException
   {
      Stack<Command> temp = new StackImpl<>(commandStack.size());
      try {
         int amountOfCommandsInTempStack = 0;
         while(commandStack.peek() != null && commandStack.peek().getUri() != uri) {
            Command a = commandStack.pop();
            a.undo();
            temp.push(a);
            amountOfCommandsInTempStack++;
         }
         Command foundIt = commandStack.pop(); //either nothing was in Stack, or uri never found
         if(foundIt == null) throw new IllegalStateException();
         foundIt.undo();

         for(int i = amountOfCommandsInTempStack; i != 0; i--) {
            Command c = temp.pop();
            c.redo();
            commandStack.push(c);
         }
         return true;
      } catch(IllegalStateException a)
      {
         a.printStackTrace();
         return false;
      }
   }

   /**
    * The user can call this method before adding Documents as well as after
    * If the user calls this method and sets the Document limit to an amount the DocStore already exceeds,
    * then it takes out the least used Documents until the conditions are reached
    * @param limit, the maximum amount of Documents allowed in DocumentStore
    */
   @Override
   public void setMaxDocumentCount(int limit)
   {
      this.documentLimit = limit;
      while (this.currentDocSize > this.documentLimit) {
         URI toRemove = minHeap.removeMin();
         Document document = (Document) bTree.get(toRemove);
         if (bTree.get(toRemove) == null) continue;
         this.deleteTrie(this.getDePuncuatedString(this.getDocument(document.getKey())), toRemove);
         currentDocSize--;
         currentByteSize -= document.getDocument().length;
         bTree.moveToDisk(toRemove);
      }

   }

   /**
    * The user can call this method before adding Documents as well as after
    * If the user calls this method and sets the byte limit to an amount the DocStore already exceeds,
    * then it takes out the least used Documents until the conditions are reached
    * @param limit, the maximum amount of bytes allowed in this store
    */
   @Override
   public void setMaxDocumentBytes(int limit)
   {
      this.byteLimit = limit;
      while (this.currentByteSize > this.byteLimit) {
         URI toRemove = minHeap.removeMin();
         Document document = (Document) bTree.get(toRemove);
         if (bTree.get(toRemove) == null) continue;
         this.deleteTrie(this.getDePuncuatedString(this.getDocument(document.getKey())), toRemove);
         currentDocSize--;
         currentByteSize -= document.getDocument().length;
         bTree.moveToDisk(toRemove);
      }
   }

}
