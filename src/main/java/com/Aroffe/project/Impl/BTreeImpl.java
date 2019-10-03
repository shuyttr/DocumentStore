package com.Aroffe.project.Impl;

import com.Aroffe.project.BTree;
import com.Aroffe.project.Document;


import java.io.File;
import java.net.URI;

//TODO make all these methods <30 lines
// also take out the fields and methods I won't use
public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree
{
   //max children per B-tree node = MAX-1 (must be an even number and greater than 2)
   private static final int MAX = 4;
   private Node root; //root of the B-tree
   private Node leftMostExternalNode;
   private int height; //height of the B-tree
   private int n; //number of key-value pairs in the B-tree
   private DocumentIOImpl documentIO;

   //B-tree node data type
   private static final class Node
   {
      private int entryCount; // number of entries
      private Entry[] entries = new Entry[BTreeImpl.MAX]; // the array of children
      private Node next;
      private Node previous;

      // create a node with k entries
      private Node(int k)
      {
         this.entryCount = k;
      }

      private void setNext(Node next)
      {
         this.next = next;
      }
      private Node getNext()
      {
         return this.next;
      }
      private void setPrevious(Node previous)
      {
         this.previous = previous;
      }
   }

   //internal nodes: only use key and child
   //external nodes: only use key and value
   public static class Entry
   {
      private Comparable key;
      private Object val;
      private Node child;
      private boolean status;

      public Entry(Comparable key, Object val, Node child)
      {
         this.key = key;
         this.val = val;
         this.child = child;
         this.status = false;
      }

      public Comparable getKey()
      {
         return this.key;
      }
      protected Object getVal(){
         return this.val;
      }
   }
   public BTreeImpl(File baseDir)
   {
      this.root = new Node(0);
      this.leftMostExternalNode = this.root;
      documentIO = new DocumentIOImpl(baseDir);
   }

   /**
    * This method always returns a Document
    * if the key returns a file then deserialize it and put the Document into memory
    * but check in DocumentStoreImpl if putting this doc back into memory will exceed the limits
    * @param k
    * @return
    */
   @Override
   public Object get(Comparable k)
   {
      URI uri = (URI) k;
      if (uri == null)
      {
         return null;
      }
      Entry entry = this.get(this.root, uri, this.height);
      if(entry != null)
      {
         if (entry.status) return null;//is status is true, it means the doc has been deleted and therefore should be returned
         if(entry.val instanceof File)
         {
            //before the Document is returned, deserialize it and then delete the File
            Document doc = documentIO.deserialize(uri);//if the value is a file, deserialize it
            ((File) entry.val).delete();//then, its file on disk must be deleted (Requirement Doc-2.2)
            return doc;
         }
         return entry.val;//if the value is Document, return it
      }
      return null;
   }

   private Entry get(Node currentNode, URI key, int height)
   {
      Entry[] entries = currentNode.entries;
      if (height == 0)
      {
         for (int j = 0; j < currentNode.entryCount; j++)
         {
            if (key.equals(entries[j].key))
            {
               //found desired key. Return its value
               return entries[j];
            }
         }
         return null;
      }
      else
      {
         for (int j = 0; j < currentNode.entryCount; j++)
         {
            if (j + 1 == currentNode.entryCount || less(key, entries[j + 1].key))
            {
               return this.get(entries[j].child, key, height - 1);
            }
         }
         //didn't find the key
         return null;
      }
   }

   @Override
   public Object put(Comparable k, Object v)
   {
      URI key = (URI) k;
      Object val = v;

      if (key == null)
      {
         throw new IllegalArgumentException("argument key to put() is null");
      }
      //if the key already exists in the b-tree, simply replace the value
      Entry alreadyThere = this.get(this.root, key, this.height);
      if(alreadyThere != null)
      {
         if(alreadyThere.val instanceof File)
         {
            Document previousValue = documentIO.deserialize(key);
            if(val == null)//if we want to delete it, it must already be there, and val here will be null put(uri, null)
            {
               //move to disk
               //make sure it doesn't get returned as a result of a get() on BTree and only an undo
               this.delete(key, alreadyThere);
            }
         }
         alreadyThere.val = val;
         return null;
      }

      Node newNode = this.put(this.root, key, (Value) val, this.height);
      this.n++;
      if (newNode == null)
      {
         return null;
      }

      Node newRoot = new Node(2);
      newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
      newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
      this.root = newRoot;
      //a split at the root always increases the tree height by 1
      this.height++;
      return null;
   }

   private boolean delete(URI key, Entry alreadyThere){
      this.moveToDisk(key);//move the document to disk
      this.setDeleteStatus(alreadyThere, true);//set the delete status to true
      return true;
   }

   private boolean setDeleteStatus(Entry entry, boolean bool)
   {
      entry.status = true;
      return true;
   }

   private Node put(Node currentNode, URI key, Value val, int height)
   {
      int j;
      Entry newEntry = new Entry(key, val, null);

      //external node
      if (height == 0)
      {
         for (j = 0; j < currentNode.entryCount; j++)
         {
            if (less(key, currentNode.entries[j].key))//what does this mean
            {
               break;
            }
         }
      }

      // internal node
      else
      {
         //find index in node entry array to insert the new entry
         for (j = 0; j < currentNode.entryCount; j++)
         {

            if ((j + 1 == currentNode.entryCount) || less(key, currentNode.entries[j + 1].key))//what does this mean
            {
               Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1);
               if (newNode == null)
               {
                  return null;
               }
               newEntry.key = newNode.entries[0].key;
               newEntry.val = null;
               newEntry.child = newNode;
               break;
            }
         }
      }
      for (int i = currentNode.entryCount; i > j; i--)
      {
         currentNode.entries[i] = currentNode.entries[i - 1];
      }
      currentNode.entries[j] = newEntry;
      currentNode.entryCount++;
      if (currentNode.entryCount < BTreeImpl.MAX)
      {
         return null;
      }
      else
      {
         return this.split(currentNode, height);
      }
   }

   private Node split(Node currentNode, int height)
   {
      Node newNode = new Node(BTreeImpl.MAX / 2);
      //by changing currentNode.entryCount, we will treat any value
      //at index higher than the new currentNode.entryCount as if
      //it doesn't exist
      currentNode.entryCount = BTreeImpl.MAX / 2;
      //copy top half of h into t
      for (int j = 0; j < BTreeImpl.MAX / 2; j++) {
         newNode.entries[j] = currentNode.entries[BTreeImpl.MAX / 2 + j];
      }
      for(int i = BTreeImpl.MAX / 2; i < BTreeImpl.MAX; i++) {
         currentNode.entries[i] = null;
      }
      //external node
      if (height == 0)
      {
         newNode.setNext(currentNode.getNext());
         newNode.setPrevious(currentNode);
         currentNode.setNext(newNode);
      }
      return newNode;
   }

   private static boolean less(Comparable k1, Comparable k2)
   {
      return k1.compareTo(k2) < 0;
   }

   /**
    * What is being passed is a URI (Comparable)
    * Step 1: we find the Document for that URI
    * Step 2: serialize that Document and save it as a file on disc
    * Step 3: re-put that "Document," really it's a File, into the Btree
    * @param k
    */
   @Override
   public void moveToDisk(Comparable k)
   {
      Document doc = (Document) this.get(k);//find the Document associated with this URI
      File file = documentIO.serialize(doc);//turn this Document into a File
      //this call uses the DocumentIO class which has all the serialization logic
      //as well as the working directory which we are in
      this.put(k,file);
   }
}
