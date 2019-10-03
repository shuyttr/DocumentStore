package com.Aroffe.project.Impl;

import com.Aroffe.project.Trie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrieImpl<Value> implements Trie<Value>
{
	private static final int alphabetSize = 26;
	private Node root;
	private Comparator<Value> comparator;
	private boolean allOrOne;

	public TrieImpl(Comparator<Value> comparator)
	{
		root = null;
		this.comparator = comparator;
		allOrOne = false;
	}
	public static class Node<Value>
	{
		protected List<Value> val;
		protected Node[] links = new Node[TrieImpl.alphabetSize];
	}
	
	public List<Value> getAllSorted(String key)
	{
		Node result = this.get(this.root, key, 0);
		if (result == null) return null;
		List<Value> finish = (List<Value>) result.val;

		if (finish == null || finish.size() == 0) return null;
		finish.sort(this.comparator);
		return finish;
	}
	
	public void put(String key, Value val)
	{
		this.root = putImplementation(this.root, key, val, 0);
	}

	@Override
	public void deleteAll(String key) {
		allOrOne = true;
		this.root = this.delete(root, key, 0, null);
	}

	private Node delete(Node x, String key, int d, Value possible)
	{
		if (x == null) return null;
		if (d == key.length())
		{
			if (allOrOne) x.val = null;
			else
			{
				x.val.remove(possible);
			}
		}
		else
		{
			int c = key.charAt(d);
			c -= 97;
			x.links[c] = delete(x.links[c], key, d+1, possible);
		}

		if(x.val != null) return x;

		for(char c = 0; c < TrieImpl.alphabetSize; c++)
		{
			if (x.links[c] != null) return x;
		}
		return null;
	}

	@Override
	public void delete(String key, Value val) {
		allOrOne = false;
		root = this.delete(this.root, key, 0, val);
	}

	private Node putImplementation(Node x, String key, Value val, int d)
	{
		if (x == null)
		{
			x = new Node(); //call to the default constructor -- which makes all links pointing to null links and its list is empty
		}
		
		if (d == key.length())
		{
			if (x.val == null) x.val = new ArrayList();
			if (x.val.contains(val)) return x; //if the document has that word in it already, there is no reason to add the Document again
			x.val.add(val);
			return x;
		}
		
		int c = key.charAt(d);
		c -= 97;
		x.links[c] = this.putImplementation(x.links[c], key, val, d + 1);
		return x;
	}
	
	private Node get(Node x, String key, int d)
	{
		if (x == null) return null;
		if (d == key.length()) return x;
		int c = key.charAt(d);
		c -= 97;
		return get(x.links[c], key, d+1);
	}

	/**
	 * Adopted from Sedgewick -- Page 736
	 * @return number of Keys in the Trie
	 */
	private int size()
	{
		return size(this.root);
	}

	private int size(Node x)
	{
		if (x == null) return 0;
		int count = 0;
		if (x.val != null) count++;
		for (char c = 0; c < TrieImpl.alphabetSize; c++)
		{
			count += size(x.links[c]);
		}
		return count;
	}

}
	

