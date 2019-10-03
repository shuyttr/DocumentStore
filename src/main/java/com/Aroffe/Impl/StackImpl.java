package main.java.com.Aroffe.Impl;

import main.java.com.Aroffe.Stack;

public class StackImpl<T> implements Stack<T>
{
    private T[] data;
    private int size;
    private int capacity;
    private int top;

    /**
     * Default Constructor, capacity 20
     */
    @SuppressWarnings("unchecked")
    public StackImpl() {
        capacity = 10;
        size = 0;
        data = (T[]) new Object[capacity];
        top = -1;
    }

    /**
     *
     * @param capacity, amount of possible elements in Stack
     */
    @SuppressWarnings("unchecked")
    public StackImpl(int capacity)
    {
        this.capacity = capacity;
        size = 0;
        data = (T[]) new Object[capacity];
        top = -1;
    }

    /**
     *
     * @param element object to add to the Stack
     */
    public void push(T element)
    {
        if (size == capacity)
        {
            this.resize();
        }
        data[++top] = element;
        size++;
    }

    /**
     *
     * @return element at top of the Stack - removes it, null if stack is empty
     */
    public T pop()
    {
        if (size == 0)
        {
            return null;
        }
        T temp = data[top];
        data[top] = null;
        top--;
        size--;
        return temp;
    }

    /**
     *
     * @return inspects top element without removal
     */
    public T peek()
    {
        if (size == 0)
        {
            return null;
        }
        return data[top];
    }

    /**
     *
     * @return amount of <I>actual</I> elements in Stack
     */
    public int size()
    {
        return this.size;
    }

    @SuppressWarnings("unchecked")
    private void resize()
    {
        this.capacity *= 2;
        T[] temp = this.data;
        data = (T[]) new Object[capacity];
        try {
            System.arraycopy(temp, 0, data, 0, temp.length);
        }
        catch(IndexOutOfBoundsException | ArrayStoreException | NullPointerException e)
        {
            e.printStackTrace();
        }
    }

}
