package com.Aroffe.project.Impl;

import com.Aroffe.project.BTree;
import com.Aroffe.project.Document;
import com.Aroffe.project.MinHeap;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public class MinHeapImpl<E extends Comparable> extends MinHeap
{
   private int size;
   private BTree<URI, Document> bTree;

   public MinHeapImpl(BTreeImpl<URI, Document> bTree)
   {
      this.count = 0;
      this.size = 2;
      this.elements = (E[]) new Comparable[this.size];
      this.elementsToArrayIndex = new HashMap<>();
      this.bTree = bTree;
   }

   /**
    * The job of reHeapify is to determine whether the Document whose time was
    * updated should stay where it is, move up in the heap, or move down in the heap,
    * and then carry out any move that should occur
    * @param element
    */
   @Override
   public void reHeapify(Comparable element)
   {
      if (this.isEmpty()) return;
      E doc = (E) element;
      int k = this.getArrayIndex(doc);
      super.upHeap(k);
      super.downHeap(k);
   }

   /**
    * This method returns the Array index in the Heap starting at index 1
    * @param element
    * @return the index of the element
    */
   @Override
   protected int getArrayIndex(Comparable element)
   {
      E retrieve = (E) element;
      if (elementsToArrayIndex.get(retrieve) == null) return 0;
      return (int) elementsToArrayIndex.get(retrieve);
   }

   /**
    * This method doubles the Array size when it runs out of capacity
    */
   @Override
   protected void doubleArraySize()
   {
      this.size = this.size * 2 + 1;
      elements = Arrays.copyOf(elements, this.size);
   }

   /**
    * Use this method to insert items into the Heap
    * @param x
    */
   @Override
   public void insert(Comparable x)
   {
      // double size of array if necessary
      if (this.count >= this.elements.length - 1)
      {
         this.doubleArraySize();
      }
      //add x to the bottom of the heap
      this.elements[++this.count] = x;
      elementsToArrayIndex.put(x, super.count);
   }

   /**
    * Use this method to move an item that may be in the wrong place, and thus needs to move down the heap
    * @param k, the index of the item to move down the heap
    */
   @Override
   protected void downHeap(int k)
   {
      while (2 * k <= this.count)
      {
         //identify which of the 2 children are smaller
         int j = 2 * k;
         if (j < this.count && this.isGreater(j, j + 1))//switch with the smaller one
         {
            j++;
         }
         //if the current value is < the smaller child, we're done, if equal then isGreater is false so then break and don't heapify
         if (!this.isGreater(k, j))
         {
            break;
         }
         //if not, swap and continue testing
         this.swap(k, j);
         //change the map to reflect the downHeap -- give the downHeap Document the new index
         //elementsToArrayIndex.put(elements[k], j); //parent point to index of the child
         //elementsToArrayIndex.put(elements[j], k); //child point to index of the parent
         k = j;
      }
   }

   protected int size()
   {
      return this.count;
   }

   /**
    * Takes out the min, and returns, and also removes it from the map
    * @return the first element in the min heap
    */
   @Override
   public E removeMin()
   {
      E result = (E) super.removeMin();
      elementsToArrayIndex.remove(result);
      return result;
   }

   protected  boolean isGreater(int i, int j)
   {
      return bTree.get((URI) this.elements[i]).compareTo(bTree.get((URI) this.elements[j])) > 0;
   }

   /**
    * Swaps two elements in the heap and changes Map<E, Integer> accordingly
    * @param i
    * @param j
    */
   @Override
   protected void swap(int i, int j)
   {
      E temp = (E) this.elements[i];
      this.elements[i] = this.elements[j];
      this.elements[j] = temp;
      elementsToArrayIndex.put(elements[i], i);
      elementsToArrayIndex.put(elements[j], j);
   }

}
