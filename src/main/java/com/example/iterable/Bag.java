package com.example.iterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A generic Bag collection backed by an ArrayList.
 * Implements the Container interface and supports full iteration.
 *
 * @param <E> the type of elements held in this Bag
 */
public class Bag<E> implements Container<E> {

    // ---------------------------------------------------------------
    // Backing data structure
    // ---------------------------------------------------------------

    private final ArrayList<E> items;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /**
     * Creates an empty Bag with a default initial capacity.
     */
    public Bag() {
        this.items = new ArrayList<>();
    }

    /**
     * Creates an empty Bag with the given initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing ArrayList
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public Bag(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(
                    "Initial capacity must be non-negative, got: " + initialCapacity);
        }
        this.items = new ArrayList<>(initialCapacity);
    }

    // ---------------------------------------------------------------
    // Container<E> interface methods
    // ---------------------------------------------------------------

    /**
     * Adds the specified item to this Bag.
     * Null items are permitted; duplicates are also permitted (Bag semantics).
     *
     * @param item the element to add
     */
    @Override
    public void add(E item) {
        items.add(item);
    }

    /**
     * Removes the first occurrence of the specified item from this Bag.
     *
     * @param item the element to remove; may be null
     * @return {@code true} if the Bag was modified (i.e., the item was found and removed)
     */
    @Override
    public boolean remove(E item) {
        return items.remove(item);
    }

    /**
     * Returns {@code true} if this Bag contains the specified item.
     *
     * @param item the element to search for; may be null
     * @return {@code true} if the item is present
     */
    @Override
    public boolean contains(E item) {
        return items.contains(item);
    }

    /**
     * Returns the number of elements currently in this Bag.
     *
     * @return element count (always &ge; 0)
     */
    @Override
    public int size() {
        return items.size();
    }

    /**
     * Returns {@code true} if this Bag contains no elements.
     *
     * @return {@code true} when size() == 0
     */
    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ---------------------------------------------------------------
    // Iterable<E> interface method
    // ---------------------------------------------------------------

    /**
     * Returns an iterator over the elements in this Bag.
     * The iterator traverses elements in insertion order.
     * The returned iterator supports {@link Iterator#remove()}.
     *
     * @return a fresh {@link Iterator} over the Bag's elements
     */
    @Override
    public Iterator<E> iterator() {
        return new BagIterator();
    }

    // ---------------------------------------------------------------
    // Private inner iterator class
    // ---------------------------------------------------------------

    /**
     * Fail-safe iterator that walks the backing ArrayList in index order.
     */
    private class BagIterator implements Iterator<E> {

        private int cursor = 0;          // index of next element to return
        private boolean removable = false; // true after a successful next()

        @Override
        public boolean hasNext() {
            return cursor < items.size();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException(
                        "No more elements in Bag (size=" + items.size() + ")");
            }
            E element = items.get(cursor);
            cursor++;
            removable = true;
            return element;
        }

        /**
         * Removes the element last returned by {@link #next()} from the Bag.
         *
         * @throws IllegalStateException if {@link #next()} has not been called,
         *                               or if remove() was already called after the last next()
         */
        @Override
        public void remove() {
            if (!removable) {
                throw new IllegalStateException(
                        "remove() must be called after next(), and only once per next() call.");
            }
            items.remove(cursor - 1);
            cursor--;           // keep cursor aligned after the shift
            removable = false;
        }
    }

    // ---------------------------------------------------------------
    // Standard Object overrides
    // ---------------------------------------------------------------

    /**
     * Returns a string representation of this Bag in the form {@code Bag[e1, e2, ...]}.
     */
    @Override
    public String toString() {
        return "Bag" + items.toString();
    }

    /**
     * Two Bags are equal when they contain the same elements in the same order.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Bag<?> other)) return false;
        return this.items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }
}