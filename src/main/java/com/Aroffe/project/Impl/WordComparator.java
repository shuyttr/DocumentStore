package com.Aroffe.project.Impl;

import com.Aroffe.project.Document;

import javax.print.Doc;
import java.net.URI;
import java.util.Comparator;

/**
 * This class is a Comparator class to compare Documents based on the amount of a specific word in that Document
 */
public class WordComparator implements Comparator<URI> {

    private String key;
    private BTreeImpl<URI, Document> btree;

    public WordComparator(BTreeImpl<URI, Document> bTree)
    {
        this.btree = bTree;
        this.key = null;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    @Override
    public int compare(URI o1, URI o2) {
        return Integer.compare(((DocumentImpl)btree.get(o2)).wordCount(this.key), ((DocumentImpl)btree.get(o1)).wordCount(this.key));
    }
}