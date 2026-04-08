package com.example.iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * An optimized generic Bag collection backed by an ArrayList.
 *
 * <h2>Optimizations over the baseline</h2>
 * <ol>
 *   <li><b>add(E)</b> – unchanged in signature, but a bulk {@code addAll(Collection)}
 *       companion is added so multiple items can be appended in one internal
 *       {@link ArrayList#addAll} call instead of looping through individual adds.
 *       {@code ArrayList.addAll} uses {@code System.arraycopy} internally, which is
 *       a single native memory-copy rather than N separate bounds-checked writes.</li>
 *
 *   <li><b>remove(E)</b> – the original scans from index 0 every time (O(n)).
 *       A {@code removeAll(E, int)} overload lets callers remove several copies in
 *       one pass. A {@code removeIf(Predicate)} companion delegates to
 *       {@link ArrayList#removeIf}, which uses a bitmask sweep and a single
 *       compaction pass — far cheaper than calling {@code remove()} in a loop.</li>
 *
 *   <li><b>contains(E)</b> – still O(n) for a general Bag, but a
 *       {@code frequency(E)} helper is added that counts occurrences in one pass
 *       rather than forcing callers to loop themselves. A short-circuit fast-path
 *       using {@code isEmpty()} is also applied so an empty Bag never enters
 *       the scan loop at all.</li>
 *
 *   <li><b>size() / isEmpty()</b> – {@code isEmpty()} is reimplemented as
 *       {@code items.isEmpty()} (unchanged) but documented to clarify it is O(1)
 *       and preferred over {@code size() == 0} in conditional checks.</li>
 *
 *   <li><b>iterator()</b> – the {@code BagIterator} is promoted to a
 *       <em>snapshot iterator</em> variant via an optional constructor flag.
 *       The default iterator still walks the live backing list (fast, no copy).
 *       When a snapshot is requested, the iterator works on an immutable copy of
 *       the element array captured at construction time, so concurrent external
 *       changes to the Bag do not corrupt ongoing traversal.</li>
 *
 *   <li><b>Memory / capacity management</b> – {@code trimToSize()} exposes the
 *       underlying {@link ArrayList#trimToSize()} so callers can reclaim wasted
 *       capacity after bulk removals. {@code ensureCapacity(int)} pre-allocates
 *       before known bulk inserts to prevent repeated array doublings.</li>
 *
 *   <li><b>Bulk / utility methods</b> – {@code clear()}, {@code sort(Comparator)},
 *       {@code toArray()}, {@code copy()}, and {@code toUnmodifiableList()} are
 *       added. None of these change {@code Container<E>} — they enrich the
 *       concrete class without touching the interface.</li>
 * </ol>
 *
 * @param <E> the type of elements held in this Bag
 */
public class Bag<E> implements Container<E> {

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    /** Default initial capacity — matches ArrayList's own default. */
    private static final int DEFAULT_CAPACITY = 10;

    // ---------------------------------------------------------------
    // Backing data structure
    // ---------------------------------------------------------------

    private final ArrayList<E> items;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /**
     * Creates an empty Bag with the default initial capacity ({@value DEFAULT_CAPACITY}).
     *
     * <p><b>Optimization:</b> Passing an explicit capacity of {@value DEFAULT_CAPACITY}
     * to the ArrayList constructor pre-allocates the internal array immediately,
     * avoiding the lazy-allocation path that ArrayList uses when constructed with
     * no arguments (which defers allocation until the first {@code add()}).
     * For a Bag that almost always receives at least one element this removes
     * one extra array allocation on first use.</p>
     */
    public Bag() {
        this.items = new ArrayList<>(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty Bag with the specified initial capacity.
     *
     * <p><b>Optimization:</b> Use this constructor when you know roughly how many
     * elements the Bag will hold. Pre-sizing eliminates the internal array-doubling
     * resize copies that would otherwise occur as the list grows past 10, 20, 40 …
     * elements. Each resize is O(n) (a {@code System.arraycopy}), so avoiding k
     * resizes saves O(n·k) work.</p>
     *
     * @param initialCapacity number of elements to pre-allocate space for
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public Bag(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(
                    "Initial capacity must be non-negative, got: " + initialCapacity);
        }
        this.items = new ArrayList<>(initialCapacity);
    }

    /**
     * Creates a Bag pre-populated with all elements from the given collection.
     *
     * <p><b>Optimization:</b> Delegates to {@link ArrayList#ArrayList(Collection)},
     * which calls {@code collection.toArray()} and then a single
     * {@code System.arraycopy} into the backing array — O(n) with the smallest
     * possible constant. This is faster than constructing an empty Bag and
     * calling {@code add()} n times because it avoids n separate bounds checks
     * and potential mid-sequence resizes.</p>
     *
     * @param source the collection whose elements populate this Bag; must not be null
     * @throws NullPointerException if {@code source} is null
     */
    public Bag(Collection<? extends E> source) {
        Objects.requireNonNull(source, "Source collection must not be null");
        this.items = new ArrayList<>(source);
    }

    // ---------------------------------------------------------------
    // Container<E> interface methods
    // ---------------------------------------------------------------

    /**
     * Appends {@code item} to the end of this Bag.
     * Null items and duplicates are both permitted.
     *
     * <p><b>Complexity:</b> Amortized O(1). When the backing array has spare
     * capacity this is a single bounds-checked array write. A resize is O(n)
     * but occurs at most O(log n) times over n total adds.</p>
     *
     * @param item the element to add; may be null
     */
    @Override
    public void add(E item) {
        items.add(item);
    }

    /**
     * Appends all elements in {@code newItems} to this Bag in the order returned
     * by the collection's iterator.
     *
     * <p><b>Optimization over repeated add():</b> {@link ArrayList#addAll} calls
     * {@code newItems.toArray()} and then a single {@code System.arraycopy} into
     * the backing array — one native memory-copy regardless of how many elements
     * are being added. Calling {@code add()} in a loop would perform n separate
     * bounds-checked writes and potentially trigger multiple resizes.</p>
     *
     * @param newItems elements to add; must not be null (elements themselves may be null)
     * @throws NullPointerException if {@code newItems} is null
     */
    public void addAll(Collection<? extends E> newItems) {
        Objects.requireNonNull(newItems, "newItems collection must not be null");
        items.addAll(newItems);
    }

    /**
     * Removes the <em>first</em> occurrence of {@code item} from this Bag.
     *
     * <p><b>Complexity:</b> O(n) — a linear scan followed by a left-shift of
     * all elements after the removed index.</p>
     *
     * <p><b>Optimization note:</b> For bulk removal, prefer {@link #removeIf}
     * or {@link #removeAll(Object, int)} rather than calling this method in a
     * loop. Those methods perform a single-pass sweep + one compaction, which
     * is O(n) total instead of O(n·k) for k individual calls.</p>
     *
     * @param item the element to remove; may be null
     * @return {@code true} if the Bag was modified
     */
    @Override
    public boolean remove(E item) {
        return items.remove(item);
    }

    /**
     * Removes up to {@code maxCount} occurrences of {@code item} from this Bag
     * in a single O(n) pass.
     *
     * <p><b>Optimization:</b> Internally this delegates to {@link ArrayList#removeIf}
     * with a stateful counter predicate. {@code ArrayList.removeIf} uses a bitmask
     * to mark elements for removal and then compacts the array in one sweep —
     * O(n) total regardless of how many elements are removed. Calling
     * {@link #remove(Object)} k times would cost O(n·k) because each call
     * rescans from the beginning and shifts elements independently.</p>
     *
     * @param item     the element to remove; may be null
     * @param maxCount maximum number of occurrences to remove; use
     *                 {@link Integer#MAX_VALUE} to remove all occurrences
     * @return number of elements actually removed (0 … maxCount)
     * @throws IllegalArgumentException if {@code maxCount} is negative
     */
    public int removeAll(E item, int maxCount) {
        if (maxCount < 0) {
            throw new IllegalArgumentException(
                    "maxCount must be non-negative, got: " + maxCount);
        }
        if (maxCount == 0 || items.isEmpty()) return 0;

        // Use an int[] to capture mutable count inside the lambda
        int[] removed = {0};
        items.removeIf(element -> {
            if (removed[0] < maxCount && Objects.equals(element, item)) {
                removed[0]++;
                return true;   // mark for removal
            }
            return false;
        });
        return removed[0];
    }

    /**
     * Removes every element that satisfies the given predicate in a single O(n) pass.
     *
     * <p><b>Optimization:</b> Delegates directly to {@link ArrayList#removeIf},
     * which uses a bitmask-sweep-and-compact strategy. This is O(n) with a very
     * small constant — the entire list is touched exactly twice (once to mark,
     * once to compact). An iterator-based removal loop would also be O(n) but
     * with a larger constant because each removal shifts subsequent elements.</p>
     *
     * @param predicate condition that identifies elements to remove; must not be null
     * @return {@code true} if any elements were removed
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "Predicate must not be null");
        return items.removeIf(predicate);
    }

    /**
     * Returns {@code true} if this Bag contains at least one occurrence of {@code item}.
     *
     * <p><b>Optimization — empty short-circuit:</b> An explicit {@code isEmpty()}
     * guard is applied before delegating to {@link ArrayList#contains}. When the
     * Bag is empty this avoids the method-dispatch chain inside {@code ArrayList}
     * and returns immediately. For non-empty bags the call still delegates to
     * the ArrayList's optimised linear scan.</p>
     *
     * <p><b>Complexity:</b> O(1) if empty; O(n) otherwise.</p>
     *
     * @param item the element to search for; may be null
     * @return {@code true} if the item is present
     */
    @Override
    public boolean contains(E item) {
        // Fast-path: skip the ArrayList scan entirely for an empty Bag
        if (items.isEmpty()) return false;
        return items.contains(item);
    }

    /**
     * Returns the number of times {@code item} appears in this Bag.
     *
     * <p><b>Optimization over manual counting:</b> This performs a single O(n)
     * pass through the backing array using a direct indexed loop (avoiding the
     * overhead of creating an Iterator object). Callers who need the count
     * should call this method once rather than combining repeated
     * {@link #contains} + {@link #remove} calls, which would cost O(n·k).</p>
     *
     * @param item the element to count; may be null
     * @return number of occurrences (0 if absent)
     */
    public int frequency(E item) {
        if (items.isEmpty()) return 0;
        int count = 0;
        // Indexed loop avoids Iterator allocation and the associated hasNext()
        // / next() virtual-dispatch overhead on every step.
        for (int i = 0, n = items.size(); i < n; i++) {
            if (Objects.equals(items.get(i), item)) count++;
        }
        return count;
    }

    /**
     * Returns the number of elements in this Bag.
     *
     * <p><b>Complexity:</b> O(1) — {@link ArrayList#size()} reads a single
     * {@code int} field.</p>
     *
     * <p><b>Usage tip:</b> Prefer {@link #isEmpty()} over {@code size() == 0}
     * in boolean contexts; both are O(1) but {@code isEmpty()} communicates
     * intent more clearly and avoids the integer comparison.</p>
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
     * <p><b>Complexity:</b> O(1) — reads {@code ArrayList.size} directly.</p>
     *
     * @return {@code true} when {@code size() == 0}
     */
    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ---------------------------------------------------------------
    // Capacity management
    // ---------------------------------------------------------------

    /**
     * Pre-allocates backing-array capacity for at least {@code minCapacity} elements.
     *
     * <p><b>Optimization:</b> Call this before a known bulk-insert sequence to
     * guarantee that the backing array is large enough. This eliminates all
     * internal resize-and-copy operations during the sequence. Each resize is
     * O(n) (a {@code System.arraycopy}), so a single {@code ensureCapacity}
     * call before inserting m elements saves up to O(n·log m) copy work.</p>
     *
     * @param minCapacity the minimum number of elements the Bag should be able
     *                    to hold without resizing
     */
    public void ensureCapacity(int minCapacity) {
        items.ensureCapacity(minCapacity);
    }

    /**
     * Shrinks the backing array to exactly match the current size.
     *
     * <p><b>Optimization:</b> After large bulk removals the ArrayList may retain
     * a much larger internal array than needed. Calling {@code trimToSize()}
     * releases that excess memory. This is particularly valuable in long-lived
     * Bags that grow to peak size and then shed most of their elements.</p>
     */
    public void trimToSize() {
        items.trimToSize();
    }

    /**
     * Removes all elements from this Bag, leaving it empty.
     *
     * <p><b>Optimization:</b> {@link ArrayList#clear()} null-fills the backing
     * array in a tight loop and resets the size field — O(n) to allow GC of
     * the element references. This is faster than removing elements one-by-one
     * through the iterator because there are no index-recalculation or
     * cursor-shift operations per element.</p>
     */
    public void clear() {
        items.clear();
    }

    // ---------------------------------------------------------------
    // Iterable<E> interface method
    // ---------------------------------------------------------------

    /**
     * Returns a <em>live</em> iterator over the elements in this Bag in
     * insertion order.
     *
     * <p>The iterator supports {@link Iterator#remove()}. It walks the backing
     * ArrayList directly — no extra allocation beyond the iterator object itself.</p>
     *
     * <p>To obtain a <em>snapshot</em> iterator that is immune to concurrent
     * modifications to the Bag, call {@link #snapshotIterator()} instead.</p>
     *
     * @return a fresh live {@link Iterator} over the Bag's elements
     */
    @Override
    public Iterator<E> iterator() {
        return new BagIterator(false);
    }

    /**
     * Returns a <em>snapshot</em> iterator that walks a private copy of the
     * element array captured at the moment this method is called.
     *
     * <p><b>Optimization for concurrent-read scenarios:</b> The snapshot is taken
     * by calling {@link ArrayList#toArray()}, which is a single
     * {@code System.arraycopy} — O(n). The iterator then walks the fixed-length
     * array rather than the live ArrayList, so any subsequent {@code add},
     * {@code remove}, or {@code clear} on the Bag will not affect this traversal.
     * This avoids the need for external synchronization or a
     * {@code CopyOnWriteArrayList} when reads vastly outnumber writes.</p>
     *
     * <p><b>Trade-off:</b> The snapshot costs O(n) memory and one copy at
     * construction time. Use the regular {@link #iterator()} when no concurrent
     * modification is expected.</p>
     *
     * <p>The snapshot iterator does <em>not</em> support {@link Iterator#remove()};
     * calling it throws {@link UnsupportedOperationException}.</p>
     *
     * @return a fresh snapshot {@link Iterator} over a copy of the Bag's elements
     */
    public Iterator<E> snapshotIterator() {
        return new BagIterator(true);
    }

    // ---------------------------------------------------------------
    // Utility / bulk operations
    // ---------------------------------------------------------------

    /**
     * Sorts the elements of this Bag in-place using the supplied comparator.
     *
     * <p><b>Optimization:</b> Delegates to {@link ArrayList#sort}, which uses
     * a Timsort variant ({@link java.util.Arrays#sort}) on the backing array
     * directly — O(n log n) with excellent cache locality because the sort
     * operates on a contiguous {@code Object[]} array. Sorting into a new
     * collection and copying back would double the memory and copy cost.</p>
     *
     * @param comparator defines the sort order; pass {@code null} to use the
     *                   elements' natural ordering (elements must implement
     *                   {@link Comparable})
     */
    public void sort(Comparator<? super E> comparator) {
        items.sort(comparator);
    }

    /**
     * Returns an unmodifiable {@link List} view of the elements in this Bag.
     *
     * <p><b>Optimization:</b> {@link Collections#unmodifiableList} wraps the
     * existing ArrayList without copying it — O(1) creation cost. This is
     * preferred over constructing a new {@code ArrayList} copy when the caller
     * only needs read access.</p>
     *
     * <p>The returned list reflects subsequent changes to the Bag. If a true
     * immutable snapshot is needed, use {@link #toArray()} or construct a
     * {@code new ArrayList<>(bag.toUnmodifiableList())} instead.</p>
     *
     * @return a live, read-only list view of this Bag's contents
     */
    public List<E> toUnmodifiableList() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns a new {@code Object[]} array containing all elements of this Bag
     * in insertion order.
     *
     * <p><b>Optimization:</b> Delegates to {@link ArrayList#toArray()}, which
     * calls {@code System.arraycopy} on the backing array — the fastest
     * possible array copy on the JVM.</p>
     *
     * @return a fresh array snapshot of this Bag's contents
     */
    public Object[] toArray() {
        return items.toArray();
    }

    /**
     * Returns a shallow copy of this Bag containing the same elements in the
     * same order.
     *
     * <p><b>Optimization:</b> Uses the {@link #Bag(Collection)} constructor,
     * which copies via a single {@code System.arraycopy} rather than
     * iterating and calling {@code add()} for each element.</p>
     *
     * @return a new Bag that is a shallow copy of this one
     */
    public Bag<E> copy() {
        return new Bag<>(this.items);
    }

    // ---------------------------------------------------------------
    // Private inner iterator class
    // ---------------------------------------------------------------

    /**
     * Dual-mode iterator:
     * <ul>
     *   <li><b>Live mode</b> ({@code snapshot=false}): walks the backing
     *       ArrayList directly via index. Supports {@link #remove()}. Zero
     *       extra allocation beyond the iterator object itself.</li>
     *   <li><b>Snapshot mode</b> ({@code snapshot=true}): copies the current
     *       element array once at construction ({@code System.arraycopy} via
     *       {@code toArray()}) and walks the copy. Does not support
     *       {@link #remove()}. Immune to concurrent modifications of the Bag.</li>
     * </ul>
     */
    private class BagIterator implements Iterator<E> {

        // In live mode: null. In snapshot mode: private copy of the element array.
        private final Object[] snapshot;

        private int cursor = 0;
        private boolean removable = false;   // only relevant in live mode
        private final boolean isSnapshot;

        /**
         * @param snapshot {@code true} → capture a snapshot array;
         *                 {@code false} → walk the live list
         */
        BagIterator(boolean snapshot) {
            this.isSnapshot = snapshot;
            this.snapshot   = snapshot ? items.toArray() : null;
        }

        @Override
        public boolean hasNext() {
            int limit = isSnapshot ? snapshot.length : items.size();
            return cursor < limit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException(
                        "No more elements in Bag (size=" + size() + ")");
            }
            E element;
            if (isSnapshot) {
                element = (E) snapshot[cursor];
            } else {
                element = items.get(cursor);
                removable = true;
            }
            cursor++;
            return element;
        }

        /**
         * Removes the element last returned by {@link #next()} from the live Bag.
         *
         * <p><b>Optimization:</b> Uses {@link ArrayList#remove(int)} (index-based)
         * rather than {@link ArrayList#remove(Object)} (value-based). The index
         * variant skips the O(n) equality scan entirely — it jumps straight to
         * the known position and shifts elements left. This is always at least as
         * fast, and for large lists with many duplicates it can be significantly
         * faster.</p>
         *
         * @throws UnsupportedOperationException if called on a snapshot iterator
         * @throws IllegalStateException if {@link #next()} has not been called,
         *         or if {@code remove()} was already called after the last {@code next()}
         */
        @Override
        public void remove() {
            if (isSnapshot) {
                throw new UnsupportedOperationException(
                        "Snapshot iterators are read-only; remove() is not supported.");
            }
            if (!removable) {
                throw new IllegalStateException(
                        "remove() must be called after next(), and only once per next() call.");
            }
            // Index-based remove: O(n) shift but NO equality scan
            items.remove(cursor - 1);
            cursor--;
            removable = false;
        }
    }

    // ---------------------------------------------------------------
    // Standard Object overrides
    // ---------------------------------------------------------------

    /**
     * Returns a string of the form {@code Bag[e1, e2, ...]}.
     */
    @Override
    public String toString() {
        return "Bag" + items.toString();
    }

    /**
     * Two Bags are equal when they contain the same elements in the same order.
     *
     * <p><b>Optimization:</b> Checks referential equality ({@code this == obj})
     * before delegating to {@link ArrayList#equals}, short-circuiting the
     * element-by-element comparison when both references point to the same object.</p>
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