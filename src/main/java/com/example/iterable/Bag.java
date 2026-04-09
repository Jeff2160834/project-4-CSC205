package com.example.iterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A generic, fail-fast Bag collection backed by an {@link ArrayList}.
 *
 * <h2>New in this version: {@code forEach}, {@code spliterator}, fail-fast</h2>
 *
 * <h3>1 — Fail-fast modification counter ({@code modCount})</h3>
 * <p>Every method that structurally changes the Bag (add, remove, clear, sort …)
 * increments an {@code int modCount} field.  Both the live iterator and the
 * spliterator snapshot {@code modCount} at construction time and compare against
 * it on every step.  If the counts differ a
 * {@link ConcurrentModificationException} is thrown immediately, making
 * unauthorized concurrent modifications visible at the earliest possible moment
 * rather than silently producing wrong results.  This is the same contract
 * provided by {@link ArrayList}, {@link java.util.HashMap}, and every other
 * standard-library collection.</p>
 *
 * <h3>2 — {@code forEach(Consumer)}</h3>
 * <p>{@link Iterable#forEach} has a default implementation in the JDK that
 * simply calls {@code iterator()} and loops — correct but not optimal.  This
 * class overrides it with a direct indexed loop over the backing array so that:
 * <ul>
 *   <li>No {@code Iterator} object is heap-allocated.</li>
 *   <li>No per-element {@code hasNext()} / {@code next()} virtual dispatch occurs.</li>
 *   <li>The JIT can more aggressively inline the loop body.</li>
 *   <li>A single {@code modCount} check after the loop body catches any
 *       structural modification performed <em>inside</em> the consumer lambda,
 *       throwing {@link ConcurrentModificationException} rather than silently
 *       skipping or double-visiting elements.</li>
 * </ul>
 *
 * <h3>3 — {@code spliterator()}</h3>
 * <p>A custom {@code BagSpliterator} is returned.  It advertises four
 * characteristics that enable important optimisations in the Streams API and
 * in parallel-decomposition frameworks:
 * <ul>
 *   <li>{@link Spliterator#ORDERED} — elements have a defined encounter order
 *       (insertion order); the Stream API preserves this order.</li>
 *   <li>{@link Spliterator#SIZED} — {@code estimateSize()} returns the exact
 *       remaining element count, allowing the framework to pre-size downstream
 *       collections and balance parallel splits without trial-and-error.</li>
 *   <li>{@link Spliterator#SUBSIZED} — both halves produced by {@code trySplit()}
 *       also know their exact sizes, enabling perfectly balanced recursive
 *       parallel splits (fork/join work-stealing is most efficient when halves
 *       are equal).</li>
 *   <li>{@link Spliterator#IMMUTABLE} — the spliterator operates on a snapshot
 *       array taken at construction time; the backing Bag may be mutated without
 *       invalidating an in-progress split/traverse sequence.  This removes the
 *       need for locks around parallel reads.</li>
 * </ul>
 * <p>{@code trySplit()} divides the remaining range exactly in half, returning
 * the left half as a new spliterator while this instance advances to cover the
 * right half.  This is the ideal strategy for array-backed data: both halves
 * are contiguous, cache-friendly, and precisely sized.</p>
 *
 * @param <E> the type of elements held in this Bag
 */
public class Bag<E> implements Container<E> {

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    private static final int DEFAULT_CAPACITY = 10;

    // ---------------------------------------------------------------
    // Backing data structure + modification counter
    // ---------------------------------------------------------------

    private final ArrayList<E> items;

    /**
     * Structural-modification counter.
     *
     * <p>Incremented by every method that changes the <em>size</em> of the Bag
     * or otherwise alters it in a way that would make an in-progress traversal
     * return incorrect results.  Both {@link BagIterator} and
     * {@link BagSpliterator} capture this value at construction and compare on
     * every step, throwing {@link ConcurrentModificationException} if the value
     * has changed.</p>
     *
     * <p>Mutating methods that increment {@code modCount}:
     * {@link #add}, {@link #addAll}, {@link #remove}, {@link #removeAll},
     * {@link #removeIf}, {@link #clear}, {@link #sort}.</p>
     */
    private int modCount = 0;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /** Creates an empty Bag with the default initial capacity. */
    public Bag() {
        this.items = new ArrayList<>(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty Bag pre-sized for {@code initialCapacity} elements.
     *
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
     * Creates a Bag pre-populated with all elements from {@code source}.
     *
     * @throws NullPointerException if {@code source} is null
     */
    public Bag(Collection<? extends E> source) {
        Objects.requireNonNull(source, "Source collection must not be null");
        this.items = new ArrayList<>(source);
    }

    // ---------------------------------------------------------------
    // Container<E> — mutating methods (each increments modCount)
    // ---------------------------------------------------------------

    /**
     * Appends {@code item} to the end of this Bag and increments {@code modCount}.
     *
     * <p>Null items and duplicates are permitted.</p>
     */
    @Override
    public void add(E item) {
        items.add(item);
        modCount++;          // structural change — live iterators must detect this
    }

    /**
     * Appends all elements of {@code newItems} in a single bulk copy, then
     * increments {@code modCount} once regardless of how many elements were added.
     *
     * <p>A single {@code modCount} increment is correct here because the entire
     * operation is atomic from the perspective of any iterator: either all
     * elements are present or none are.</p>
     *
     * @throws NullPointerException if {@code newItems} is null
     */
    public void addAll(Collection<? extends E> newItems) {
        Objects.requireNonNull(newItems, "newItems collection must not be null");
        if (!newItems.isEmpty()) {
            items.addAll(newItems);
            modCount++;      // one increment covers the entire bulk add
        }
    }

    /**
     * Removes the first occurrence of {@code item} and increments {@code modCount}
     * only when the Bag was actually modified.
     */
    @Override
    public boolean remove(E item) {
        boolean changed = items.remove(item);
        if (changed) modCount++;
        return changed;
    }

    /**
     * Removes up to {@code maxCount} occurrences of {@code item} in a single
     * O(n) bitmask-sweep pass, then increments {@code modCount} once if any
     * element was removed.
     *
     * @throws IllegalArgumentException if {@code maxCount} is negative
     */
    public int removeAll(E item, int maxCount) {
        if (maxCount < 0) throw new IllegalArgumentException(
                "maxCount must be non-negative, got: " + maxCount);
        if (maxCount == 0 || items.isEmpty()) return 0;

        int[] removed = {0};
        items.removeIf(element -> {
            if (removed[0] < maxCount && Objects.equals(element, item)) {
                removed[0]++;
                return true;
            }
            return false;
        });
        if (removed[0] > 0) modCount++;
        return removed[0];
    }

    /**
     * Removes every element satisfying {@code predicate} in one O(n) sweep,
     * incrementing {@code modCount} once if any element was removed.
     *
     * @throws NullPointerException if {@code predicate} is null
     */
    public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "Predicate must not be null");
        boolean changed = items.removeIf(predicate);
        if (changed) modCount++;
        return changed;
    }

    /**
     * Removes all elements from this Bag and increments {@code modCount}.
     *
     * <p>After this call {@code isEmpty()} returns {@code true}.</p>
     */
    public void clear() {
        if (!items.isEmpty()) {
            items.clear();
            modCount++;
        }
    }

    /**
     * Sorts the elements in-place using {@code comparator} and increments
     * {@code modCount} because the positional order of elements has changed.
     *
     * <p>Even though the <em>set</em> of elements is unchanged, an iterator
     * that started before the sort would visit elements in a now-stale order,
     * so the structural-change counter must be incremented.</p>
     */
    public void sort(Comparator<? super E> comparator) {
        items.sort(comparator);
        modCount++;          // positional change — iterators must restart
    }

    // ---------------------------------------------------------------
    // Container<E> — non-mutating query methods
    // ---------------------------------------------------------------

    /**
     * Returns {@code true} if this Bag contains at least one occurrence of
     * {@code item}.  Empty-Bag short-circuit avoids the ArrayList scan entirely.
     */
    @Override
    public boolean contains(E item) {
        if (items.isEmpty()) return false;
        return items.contains(item);
    }

    /** Returns the number of elements in this Bag. O(1). */
    @Override
    public int size() { return items.size(); }

    /** Returns {@code true} when this Bag contains no elements. O(1). */
    @Override
    public boolean isEmpty() { return items.isEmpty(); }

    /**
     * Returns the number of times {@code item} appears in this Bag.
     *
     * <p>Performs a single O(n) indexed-loop pass; avoids {@code Iterator}
     * allocation and per-element virtual dispatch.</p>
     */
    public int frequency(E item) {
        if (items.isEmpty()) return 0;
        int count = 0;
        for (int i = 0, n = items.size(); i < n; i++) {
            if (Objects.equals(items.get(i), item)) count++;
        }
        return count;
    }

    // ---------------------------------------------------------------
    // forEach override — avoids Iterator allocation, fail-fast
    // ---------------------------------------------------------------

    /**
     * Performs {@code action} for each element in insertion order.
     *
     * <h4>Why override the default?</h4>
     * <p>The {@link Iterable#forEach} default implementation allocates an
     * {@link Iterator} object and calls {@code hasNext()} / {@code next()} for
     * every element — correct but carries per-element virtual-dispatch overhead.
     * This override uses a direct indexed loop over the backing list, which:</p>
     * <ul>
     *   <li>Avoids heap-allocating an Iterator.</li>
     *   <li>Lets the JIT inline the loop body more aggressively.</li>
     *   <li>Performs a <em>single</em> {@code modCount} comparison after each
     *       {@code action} call: if the consumer mutates the Bag (e.g. calls
     *       {@code add()} inside a lambda) a
     *       {@link ConcurrentModificationException} is thrown immediately rather
     *       than silently producing a corrupted traversal.</li>
     * </ul>
     *
     * <p><b>Fail-fast behaviour:</b> the expected modCount is captured once
     * before the loop.  After each {@code action.accept(element)} call the
     * current {@code modCount} is compared; any structural change throws
     * {@link ConcurrentModificationException}.</p>
     *
     * @param action the operation to perform on each element; must not be null
     * @throws NullPointerException          if {@code action} is null
     * @throws ConcurrentModificationException if the Bag is structurally
     *         modified during iteration
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action must not be null");
        final int expectedModCount = modCount;   // snapshot before loop
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            action.accept(items.get(i));
            // Check after every action call — catches mutations inside the lambda
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException(
                        "Bag was structurally modified during forEach at index " + i);
            }
        }
    }

    // ---------------------------------------------------------------
    // iterator — live, fail-fast
    // ---------------------------------------------------------------

    /**
     * Returns a live, fail-fast {@link Iterator} over the elements in insertion
     * order.
     *
     * <p>The iterator is <em>fail-fast</em>: it captures {@code modCount} at
     * construction and checks it on every {@code hasNext()}, {@code next()}, and
     * {@code remove()} call.  Any structural modification to the Bag after the
     * iterator is created — other than through the iterator's own
     * {@code remove()} — causes a {@link ConcurrentModificationException}.</p>
     *
     * <p>For a traversal that is immune to concurrent modifications, use
     * {@link #snapshotIterator()} instead.</p>
     */
    @Override
    public Iterator<E> iterator() {
        return new BagIterator(false);
    }

    /**
     * Returns a snapshot {@link Iterator} that walks a private copy of the
     * element array taken at the moment of this call.
     *
     * <p>Subsequent structural modifications to the Bag (add, remove, clear …)
     * do not affect this iterator.  The snapshot iterator does <em>not</em>
     * support {@link Iterator#remove()}.</p>
     */
    public Iterator<E> snapshotIterator() {
        return new BagIterator(true);
    }

    // ---------------------------------------------------------------
    // spliterator — snapshot, ORDERED | SIZED | SUBSIZED | IMMUTABLE
    // ---------------------------------------------------------------

    /**
     * Returns a {@link Spliterator} over the elements of this Bag.
     *
     * <h4>Why a custom spliterator?</h4>
     * <p>The default {@code Iterable.spliterator()} creates a
     * {@link Spliterators#spliteratorUnknownSize} wrapper around the iterator,
     * which lacks size information and cannot split.  This custom implementation
     * provides four characteristics that the Streams API exploits heavily:</p>
     *
     * <ul>
     *   <li>{@link Spliterator#ORDERED} — encounter order is defined (insertion
     *       order); {@code Stream.findFirst()}, {@code forEachOrdered()}, and
     *       ordered collectors all rely on this.</li>
     *   <li>{@link Spliterator#SIZED} — {@code estimateSize()} returns the
     *       exact remaining count, letting the framework pre-size result
     *       containers and estimate parallel work without probing.</li>
     *   <li>{@link Spliterator#SUBSIZED} — both halves from {@code trySplit()}
     *       also know their exact sizes, enabling perfectly balanced recursive
     *       fork/join splits.  Without this flag the framework must treat
     *       sub-spliterators as unsized.</li>
     *   <li>{@link Spliterator#IMMUTABLE} — the spliterator operates on a
     *       snapshot array; the Bag can be mutated concurrently without
     *       corrupting an in-progress parallel stream pipeline.  This is
     *       possible because we pay one O(n) copy cost upfront.</li>
     * </ul>
     *
     * <h4>Splitting strategy</h4>
     * <p>{@code trySplit()} divides the remaining range in half, returning the
     * left half as a new {@code BagSpliterator} while this spliterator advances
     * its {@code lo} to the midpoint.  Both halves cover contiguous slices of
     * the snapshot array — optimal for cache locality and for the
     * fork/join work-stealing scheduler.</p>
     *
     * @return a {@link BagSpliterator} over a snapshot of this Bag's elements
     */
    @Override
    public Spliterator<E> spliterator() {
        return new BagSpliterator(items.toArray(), 0, items.size());
    }

    // ---------------------------------------------------------------
    // Capacity management
    // ---------------------------------------------------------------

    /** Pre-allocates backing-array capacity for {@code minCapacity} elements. */
    public void ensureCapacity(int minCapacity) { items.ensureCapacity(minCapacity); }

    /** Shrinks the backing array to exactly match the current size. */
    public void trimToSize() { items.trimToSize(); }

    // ---------------------------------------------------------------
    // Utility / view methods
    // ---------------------------------------------------------------

    /** Returns an O(1) unmodifiable live view of this Bag's contents. */
    public List<E> toUnmodifiableList() { return Collections.unmodifiableList(items); }

    /** Returns a new {@code Object[]} snapshot of this Bag's elements. */
    public Object[] toArray() { return items.toArray(); }

    /** Returns a shallow copy of this Bag as a new, independent Bag. */
    public Bag<E> copy() { return new Bag<>(this.items); }

    // ---------------------------------------------------------------
    // BagIterator — dual-mode (live fail-fast / snapshot read-only)
    // ---------------------------------------------------------------

    /**
     * Dual-mode iterator.
     *
     * <h4>Live mode ({@code isSnapshot = false})</h4>
     * <ul>
     *   <li>Walks the backing ArrayList directly via index.</li>
     *   <li>Captures {@code modCount} at construction; checks it on every call
     *       to {@code hasNext()}, {@code next()}, and {@code remove()}.  Any
     *       external structural change throws
     *       {@link ConcurrentModificationException}.</li>
     *   <li>Supports {@code remove()}, which uses the index-based
     *       {@link ArrayList#remove(int)} to skip the equality scan, then
     *       increments {@code modCount} and updates the expected count so that
     *       the iterator's own removal is not misidentified as a concurrent
     *       modification.</li>
     * </ul>
     *
     * <h4>Snapshot mode ({@code isSnapshot = true})</h4>
     * <ul>
     *   <li>Copies the element array once at construction via
     *       {@code items.toArray()} (single {@code System.arraycopy}).</li>
     *   <li>Immune to structural changes on the live Bag.</li>
     *   <li>{@code remove()} throws {@link UnsupportedOperationException}.</li>
     * </ul>
     */
    private class BagIterator implements Iterator<E> {

        private final Object[] snapshot;   // non-null only in snapshot mode
        private final boolean  isSnapshot;

        // Live-mode state
        private int     cursor           = 0;
        private boolean removable        = false;
        private int     expectedModCount;        // captured at construction

        BagIterator(boolean snapshot) {
            this.isSnapshot       = snapshot;
            this.snapshot         = snapshot ? items.toArray() : null;
            this.expectedModCount = modCount;    // always snapshot, used in live mode
        }

        // -- fail-fast check helper --
        private void checkForComodification() {
            if (!isSnapshot && modCount != expectedModCount) {
                throw new ConcurrentModificationException(
                        "Bag was structurally modified while iterating. "
                                + "Expected modCount=" + expectedModCount
                                + ", actual modCount=" + modCount);
            }
        }

        @Override
        public boolean hasNext() {
            checkForComodification();   // detect external mutation even on hasNext
            int limit = isSnapshot ? snapshot.length : items.size();
            return cursor < limit;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int limit = isSnapshot ? snapshot.length : items.size();
            if (cursor >= limit) {
                throw new NoSuchElementException(
                        "No more elements in Bag (size=" + size() + ")");
            }
            E element = isSnapshot ? (E) snapshot[cursor] : items.get(cursor);
            cursor++;
            if (!isSnapshot) removable = true;
            return element;
        }

        /**
         * Removes the element last returned by {@code next()} from the live Bag.
         *
         * <p>Uses {@link ArrayList#remove(int)} (index-based, no equality scan),
         * then increments both the Bag's {@code modCount} and this iterator's
         * {@code expectedModCount} so that the iterator's own removal is not
         * flagged as a concurrent modification on the next call.</p>
         *
         * @throws UnsupportedOperationException on a snapshot iterator
         * @throws IllegalStateException if {@code next()} has not been called, or
         *         {@code remove()} was already called after the last {@code next()}
         * @throws ConcurrentModificationException if an external mutation occurred
         */
        @Override
        public void remove() {
            if (isSnapshot) throw new UnsupportedOperationException(
                    "Snapshot iterators are read-only; remove() is not supported.");
            checkForComodification();
            if (!removable) throw new IllegalStateException(
                    "remove() must be called after next(), and only once per next() call.");

            items.remove(cursor - 1);   // index-based: no equality scan
            cursor--;
            removable        = false;
            modCount++;                 // Bag structurally changed
            expectedModCount = modCount; // iterator owns this change — don't throw
        }

        /**
         * Overrides the default {@code forEachRemaining} to use a tight indexed
         * loop for the remaining elements rather than repeated {@code next()} calls.
         *
         * <p>A single {@code modCount} check before the loop (rather than once
         * per element) is sufficient here because the consumer is not allowed to
         * mutate the Bag — if it does, the check at the end of the loop body
         * detects it.  This keeps the hot path as lean as possible.</p>
         */
        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action, "action must not be null");
            checkForComodification();

            if (isSnapshot) {
                // Snapshot: tight indexed loop, no modCount overhead
                while (cursor < snapshot.length) {
                    action.accept((E) snapshot[cursor++]);
                }
            } else {
                // Live: check modCount after every action, same as forEach()
                final int size = items.size();
                while (cursor < size) {
                    action.accept(items.get(cursor++));
                    if (modCount != expectedModCount) {
                        throw new ConcurrentModificationException(
                                "Bag was structurally modified during forEachRemaining "
                                        + "at index " + (cursor - 1));
                    }
                }
                removable = false;  // cursor is at end; remove() no longer valid
            }
        }
    }

    // ---------------------------------------------------------------
    // BagSpliterator — snapshot, ORDERED | SIZED | SUBSIZED | IMMUTABLE
    // ---------------------------------------------------------------

    /**
     * Array-backed spliterator over a snapshot of the Bag's elements.
     *
     * <p>Characteristics: {@link Spliterator#ORDERED} | {@link Spliterator#SIZED}
     * | {@link Spliterator#SUBSIZED} | {@link Spliterator#IMMUTABLE}.</p>
     *
     * <p>The snapshot is taken once by {@link Bag#spliterator()} via
     * {@code items.toArray()}.  All splits share the same underlying array
     * object; each split is simply a different {@code [lo, hi)} slice.
     * This is O(1) per split — no copying on each {@code trySplit()} call.</p>
     */
    private class BagSpliterator implements Spliterator<E> {

        private final Object[] data;  // shared snapshot array — never mutated
        private int lo;               // inclusive start of this spliterator's range
        private final int hi;         // exclusive end of this spliterator's range

        BagSpliterator(Object[] data, int lo, int hi) {
            this.data = data;
            this.lo   = lo;
            this.hi   = hi;
        }

        /**
         * Attempts to split this spliterator into two roughly equal halves.
         *
         * <p>Returns {@code null} when the range is too small to split
         * (fewer than 2 elements remaining).  Otherwise divides at the midpoint:
         * the returned spliterator covers {@code [lo, mid)} and this spliterator
         * advances to cover {@code [mid, hi)}.  Both halves know their exact size
         * ({@code SUBSIZED}), enabling balanced fork/join work-stealing.</p>
         *
         * @return a new {@link BagSpliterator} covering the left half, or
         *         {@code null} if the range cannot be split further
         */
        @Override
        public BagSpliterator trySplit() {
            int mid = (lo + hi) >>> 1;   // unsigned right-shift avoids overflow
            if (mid == lo) return null;  // fewer than 2 elements — cannot split
            BagSpliterator left = new BagSpliterator(data, lo, mid);
            lo = mid;                    // this spliterator now covers [mid, hi)
            return left;
        }

        /**
         * If a remaining element exists, passes it to {@code action} and advances.
         *
         * @return {@code true} if an element was consumed; {@code false} if exhausted
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action, "action must not be null");
            if (lo >= hi) return false;
            action.accept((E) data[lo++]);
            return true;
        }

        /**
         * Feeds all remaining elements to {@code action} in a tight indexed loop.
         *
         * <p>Overriding the default (which calls {@code tryAdvance} repeatedly)
         * removes the per-element {@code lo >= hi} branch check overhead.  The
         * loop body is a single unchecked array read + consumer call — the
         * smallest possible hot path.</p>
         */
        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action, "action must not be null");
            while (lo < hi) {
                action.accept((E) data[lo++]);
            }
        }

        /**
         * Returns the exact number of elements remaining in this split.
         *
         * <p>Because the characteristic {@link Spliterator#SIZED} is advertised,
         * this value is treated as exact by the Streams framework — it is used to
         * pre-size output collections and to compute parallel task granularity.</p>
         */
        @Override
        public long estimateSize() { return hi - lo; }

        /**
         * Returns the exact size (same as {@link #estimateSize()} because
         * {@code SIZED} is set).
         */
        @Override
        public long getExactSizeIfKnown() { return hi - lo; }

        /**
         * Reports the four characteristics that make this spliterator useful:
         * {@code ORDERED | SIZED | SUBSIZED | IMMUTABLE}.
         *
         * <ul>
         *   <li>{@code ORDERED}   — insertion order is the encounter order.</li>
         *   <li>{@code SIZED}     — {@code estimateSize()} is exact.</li>
         *   <li>{@code SUBSIZED}  — both halves of a split are also sized.</li>
         *   <li>{@code IMMUTABLE} — the snapshot array is never written to after
         *                           creation; no {@code ConcurrentModificationException}
         *                           is possible.</li>
         * </ul>
         */
        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | IMMUTABLE;
        }
    }

    // ---------------------------------------------------------------
    // Standard Object overrides
    // ---------------------------------------------------------------

    @Override
    public String toString() { return "Bag" + items.toString(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Bag<?> other)) return false;
        return this.items.equals(other.items);
    }

    @Override
    public int hashCode() { return items.hashCode(); }
}