package main.java.com.Aroffe;

public interface BTree<Key extends Comparable<Key>, Value>
{
   Value get(Key k);
   Value put(Key k, Value v);
   void moveToDisk(Key k) throws Exception;
}
