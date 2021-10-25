package com.pump.awt.geom;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * This lets you add elements to a public inner array, but it otherwise
 * doesn't pretend to be a formal java.util.List. The theory is: using
 * any java.util.List adds small amounts of overhead that we can avoid if
 * we just access array elements directly.
 */
class ExposedArrayWrapper<T> implements Serializable {

    private static Object[] EMPTY = new Object[0];

    private static final long serialVersionUID = 1;

    /**
     * The array buffer into which the components of the list are
     * stored. The capacity of the list is the length of this array buffer,
     * and is at least large enough to contain all the list elements.<p>
     * This array must be of type <code>type</code>.<p>
     * Any array elements following the last element in the list are null.
     */
    public T[] elementData;

    /**
     * The number of valid components in this list.
     * Components <tt>elementData[0]</tt> through
     * <tt>elementData[elementCount-1]</tt> are the actual items.
     */
    public int elementCount;

    public Class<T> elementType;

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param   initialCapacity     the initial capacity of the list.
     * @exception IllegalArgumentException if the specified initial capacity
     *               is negative
     */
    public ExposedArrayWrapper(Class<T> elementType, int initialCapacity) {
        this.elementType = elementType;
        if(initialCapacity==0) {
            this.elementData = (T[]) EMPTY;
        } else {
            this.elementData = (T[]) Array.newInstance(elementType, initialCapacity);
        }
    }

    public ExposedArrayWrapper(Class<T> elementType) {
        this(elementType, 10);
    }

    /**
     * Increases the capacity of this list, if necessary, to ensure
     * that it can hold at least the number of components specified by
     * the minimum capacity argument.
     *
     * <p>If the current capacity of this list is less than
     * <tt>minCapacity</tt>, then its capacity is increased by replacing its
     * internal data array, kept in the field <tt>elementData</tt>, with a
     * larger one.  The size of the new data array will be the old size plus
     * <tt>capacityIncrement</tt>, unless the value of
     * <tt>capacityIncrement</tt> is less than or equal to zero, in which case
     * the new capacity will be twice the old capacity; but if this new size
     * is still smaller than <tt>minCapacity</tt>, then the new capacity will
     * be <tt>minCapacity</tt>.
     *
     * @param minCapacity the desired minimum capacity.
     */
    private void ensureCapacity(int minCapacity) {
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            Object oldData[] = elementData;

            int newCapacity =  (oldCapacity * 2);
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }

            elementData = (T[]) Array.newInstance(elementType, newCapacity);
            System.arraycopy(oldData, 0, elementData, 0, elementCount);
        }
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    public void clear() {
        // Let gc do its work
        for (int i = 0; i < elementCount; i++)
            elementData[i] = null;

        elementCount = 0;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param o element to be appended to this list.
     */
    public void add(T o) {
        ensureCapacity(elementCount + 1);
        elementData[elementCount++] = o;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(0); // internal version
        out.writeObject(elementType);
        out.writeInt(elementCount);
        out.writeObject(elementData);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        int version = in.readInt();
        if (version == 0) {
            elementType = (Class<T>) in.readObject();
            elementCount = in.readInt();
            elementData = (T[]) in.readObject();
        } else {
            throw new RuntimeException("Unsupported internal version: " + version);
        }
    }
}
