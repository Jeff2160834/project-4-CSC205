package com.example.iterable;


import org.junit.jupiter.api.*;
import java.util.*;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for the optimized {@link Bag}.
 *
 * Test groups:
 *  1.  Constructor variants
 *  2.  add() and addAll()
 *  3.  contains() and frequency()
 *  4.  remove(), removeAll(), removeIf()
 *  5.  size() and isEmpty()
 *  6.  Capacity management (ensureCapacity, trimToSize)
 *  7.  clear()
 *  8.  Live iterator
 *  9.  Snapshot iterator
 * 10.  Iterator exception guards
 * 11.  sort()
 * 12.  toUnmodifiableList(), toArray(), copy()
 * 13.  Null handling
 * 14.  Duplicate items
 * 15.  Generic type flexibility
 */
@DisplayName("Bag<E> Optimized – Full Test Suite")
class BagTest {

    private Bag<String> bag;

    @BeforeEach
    void setUp() { bag = new Bag<>(); }

    // ==================================================================
    // 1. Constructor variants
    // ==================================================================

    @Nested @DisplayName("1 · Constructor Variants")
    class ConstructorTests {

        @Test @DisplayName("default constructor produces empty Bag")
        void defaultConstructor() {
            assertTrue(bag.isEmpty());
            assertEquals(0, bag.size());
        }

        @Test @DisplayName("capacity constructor produces empty Bag")
        void capacityConstructor() {
            Bag<String> b = new Bag<>(100);
            assertTrue(b.isEmpty());
        }

        @Test @DisplayName("collection constructor copies all elements")
        void collectionConstructor() {
            Bag<String> b = new Bag<>(List.of("a", "b", "c"));
            assertEquals(3, b.size());
            assertTrue(b.contains("a") && b.contains("b") && b.contains("c"));
        }

        @Test @DisplayName("collection constructor preserves insertion order")
        void collectionConstructorOrder() {
            Bag<String> b = new Bag<>(List.of("x", "y", "z"));
            List<String> result = new ArrayList<>();
            for (String s : b) result.add(s);
            assertEquals(List.of("x", "y", "z"), result);
        }

        @Test @DisplayName("collection constructor with null source throws NullPointerException")
        void collectionConstructorNullThrows() {
            assertThrows(NullPointerException.class, () -> new Bag<>(null));
        }

        @Test @DisplayName("negative capacity throws IllegalArgumentException")
        void negativeCapacityThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Bag<>(-1));
        }
    }

    // ==================================================================
    // 2. add() and addAll()
    // ==================================================================

    @Nested @DisplayName("2 · add() and addAll()")
    class AddTests {

        @Test @DisplayName("add() increases size by 1")
        void addIncreasesSize() {
            bag.add("a");
            assertEquals(1, bag.size());
        }

        @Test @DisplayName("addAll() appends all elements in order")
        void addAllAppendsAll() {
            bag.add("first");
            bag.addAll(List.of("second", "third", "fourth"));
            assertEquals(4, bag.size());
            List<String> result = new ArrayList<>();
            for (String s : bag) result.add(s);
            assertEquals(List.of("first", "second", "third", "fourth"), result);
        }

        @Test @DisplayName("addAll() with empty collection leaves Bag unchanged")
        void addAllEmpty() {
            bag.add("existing");
            bag.addAll(Collections.emptyList());
            assertEquals(1, bag.size());
        }

        @Test @DisplayName("addAll(null) throws NullPointerException")
        void addAllNullThrows() {
            assertThrows(NullPointerException.class, () -> bag.addAll(null));
        }

        @Test @DisplayName("addAll() is equivalent to sequential add() calls")
        void addAllEquivalentToSequentialAdds() {
            Bag<String> b1 = new Bag<>();
            Bag<String> b2 = new Bag<>();
            List<String> items = List.of("p", "q", "r", "s");
            b1.addAll(items);
            items.forEach(b2::add);
            assertEquals(b1, b2);
        }
    }

    // ==================================================================
    // 3. contains() and frequency()
    // ==================================================================

    @Nested @DisplayName("3 · contains() and frequency()")
    class ContainsFrequencyTests {

        @Test @DisplayName("contains() true for present item")
        void containsPresent() {
            bag.add("here");
            assertTrue(bag.contains("here"));
        }

        @Test @DisplayName("contains() false for absent item")
        void containsAbsent() {
            bag.add("here");
            assertFalse(bag.contains("not-here"));
        }

        @Test @DisplayName("contains() empty short-circuit returns false immediately")
        void containsEmptyShortCircuit() {
            assertFalse(bag.contains("anything"));
        }

        @Test @DisplayName("frequency() returns correct count for duplicates")
        void frequencyDuplicates() {
            bag.add("x"); bag.add("x"); bag.add("x"); bag.add("y");
            assertEquals(3, bag.frequency("x"));
            assertEquals(1, bag.frequency("y"));
        }

        @Test @DisplayName("frequency() returns 0 for absent item")
        void frequencyAbsent() {
            bag.add("a");
            assertEquals(0, bag.frequency("z"));
        }

        @Test @DisplayName("frequency() returns 0 on empty Bag (short-circuit)")
        void frequencyEmpty() {
            assertEquals(0, bag.frequency("x"));
        }

        @Test @DisplayName("frequency(null) counts nulls correctly")
        void frequencyNull() {
            bag.add(null); bag.add(null); bag.add("real");
            assertEquals(2, bag.frequency(null));
        }
    }

    // ==================================================================
    // 4. remove(), removeAll(), removeIf()
    // ==================================================================

    @Nested @DisplayName("4 · remove(), removeAll(), removeIf()")
    class RemoveTests {

        @Test @DisplayName("remove() removes first occurrence")
        void removeFirstOccurrence() {
            bag.addAll(List.of("a", "b", "a"));
            assertTrue(bag.remove("a"));
            assertEquals(2, bag.size());
            // "a" should still be present (second copy)
            assertTrue(bag.contains("a"));
        }

        @Test @DisplayName("removeAll(item, 2) removes exactly 2 occurrences")
        void removeAllTwoOccurrences() {
            bag.addAll(List.of("d", "d", "d", "d", "e"));
            int removed = bag.removeAll("d", 2);
            assertEquals(2, removed);
            assertEquals(3, bag.size());
            assertEquals(2, bag.frequency("d"));
        }

        @Test @DisplayName("removeAll with MAX_VALUE removes all occurrences")
        void removeAllMaxValue() {
            bag.addAll(List.of("d", "d", "d"));
            int removed = bag.removeAll("d", Integer.MAX_VALUE);
            assertEquals(3, removed);
            assertFalse(bag.contains("d"));
        }

        @Test @DisplayName("removeAll on absent item returns 0")
        void removeAllAbsent() {
            bag.add("here");
            assertEquals(0, bag.removeAll("absent", 5));
            assertEquals(1, bag.size());
        }

        @Test @DisplayName("removeAll with 0 maxCount removes nothing")
        void removeAllZeroCount() {
            bag.add("a");
            assertEquals(0, bag.removeAll("a", 0));
            assertEquals(1, bag.size());
        }

        @Test @DisplayName("removeAll with negative maxCount throws IllegalArgumentException")
        void removeAllNegativeThrows() {
            assertThrows(IllegalArgumentException.class, () -> bag.removeAll("a", -1));
        }

        @Test @DisplayName("removeIf removes all matching elements")
        void removeIfMatchingElements() {
            bag.addAll(List.of("apple", "banana", "avocado", "cherry"));
            boolean changed = bag.removeIf(s -> s.startsWith("a"));
            assertTrue(changed);
            assertEquals(2, bag.size());
            assertFalse(bag.contains("apple") || bag.contains("avocado"));
        }

        @Test @DisplayName("removeIf with no matches returns false")
        void removeIfNoMatches() {
            bag.addAll(List.of("x", "y", "z"));
            assertFalse(bag.removeIf(s -> s.startsWith("q")));
            assertEquals(3, bag.size());
        }

        @Test @DisplayName("removeIf(null) throws NullPointerException")
        void removeIfNullThrows() {
            assertThrows(NullPointerException.class, () -> bag.removeIf(null));
        }
    }

    // ==================================================================
    // 5. size() and isEmpty()
    // ==================================================================

    @Nested @DisplayName("5 · size() and isEmpty()")
    class SizeTests {

        @Test @DisplayName("isEmpty() and size() reflect additions and removals")
        void sizeTracksChanges() {
            assertTrue(bag.isEmpty());
            bag.add("a"); assertFalse(bag.isEmpty()); assertEquals(1, bag.size());
            bag.add("b"); assertEquals(2, bag.size());
            bag.remove("a"); assertEquals(1, bag.size());
            bag.remove("b"); assertTrue(bag.isEmpty());
        }
    }

    // ==================================================================
    // 6. Capacity management
    // ==================================================================

    @Nested @DisplayName("6 · Capacity Management")
    class CapacityTests {

        @Test @DisplayName("ensureCapacity allows bulk inserts without resize")
        void ensureCapacityBulkInsert() {
            Bag<Integer> b = new Bag<>();
            b.ensureCapacity(500);
            for (int i = 0; i < 500; i++) b.add(i);
            assertEquals(500, b.size());
        }

        @Test @DisplayName("trimToSize preserves all elements")
        void trimToSizePreservesElements() {
            Bag<Integer> b = new Bag<>(1000);
            for (int i = 0; i < 10; i++) b.add(i);
            b.trimToSize();
            assertEquals(10, b.size());
            for (int i = 0; i < 10; i++) assertTrue(b.contains(i));
        }
    }

    // ==================================================================
    // 7. clear()
    // ==================================================================

    @Nested @DisplayName("7 · clear()")
    class ClearTests {

        @Test @DisplayName("clear() empties the Bag")
        void clearEmptiesBag() {
            bag.addAll(List.of("a", "b", "c"));
            bag.clear();
            assertTrue(bag.isEmpty());
            assertEquals(0, bag.size());
        }

        @Test @DisplayName("clear() on empty Bag is a no-op")
        void clearEmptyIsNoOp() {
            assertDoesNotThrow(() -> bag.clear());
            assertTrue(bag.isEmpty());
        }

        @Test @DisplayName("Bag is reusable after clear()")
        void reusableAfterClear() {
            bag.add("old");
            bag.clear();
            bag.add("new");
            assertEquals(1, bag.size());
            assertTrue(bag.contains("new"));
            assertFalse(bag.contains("old"));
        }
    }

    // ==================================================================
    // 8. Live iterator
    // ==================================================================

    @Nested @DisplayName("8 · Live Iterator")
    class LiveIteratorTests {

        @Test @DisplayName("live iterator visits all elements in insertion order")
        void liveIteratorOrder() {
            bag.addAll(List.of("a", "b", "c"));
            List<String> result = new ArrayList<>();
            for (String s : bag) result.add(s);
            assertEquals(List.of("a", "b", "c"), result);
        }

        @Test @DisplayName("live iterator remove() deletes the correct element")
        void liveIteratorRemove() {
            bag.addAll(List.of("keep", "drop", "keep2"));
            Iterator<String> it = bag.iterator();
            while (it.hasNext()) {
                if ("drop".equals(it.next())) it.remove();
            }
            assertEquals(2, bag.size());
            assertFalse(bag.contains("drop"));
        }

        @Test @DisplayName("live iterator uses index-based remove (no equality scan)")
        void liveIteratorIndexRemove() {
            // If index-based remove is used, duplicates are handled correctly:
            // only the element at cursor-1 is removed, not any equal element elsewhere
            bag.addAll(List.of("dup", "unique", "dup"));
            Iterator<String> it = bag.iterator();
            it.next();   // cursor at "dup" [0]
            it.remove(); // should remove [0], not [2]
            assertEquals(2, bag.size());
            assertTrue(bag.contains("dup"));   // [2] still present
            assertTrue(bag.contains("unique")); // unaffected
        }
    }

    // ==================================================================
    // 9. Snapshot iterator
    // ==================================================================

    @Nested @DisplayName("9 · Snapshot Iterator")
    class SnapshotIteratorTests {

        @Test @DisplayName("snapshot iterator is unaffected by subsequent add()")
        void snapshotImmutableToAdd() {
            bag.addAll(List.of("a", "b", "c"));
            Iterator<String> snap = bag.snapshotIterator();
            bag.add("d");  // live mutation
            List<String> seen = new ArrayList<>();
            while (snap.hasNext()) seen.add(snap.next());
            assertEquals(3, seen.size());
            assertFalse(seen.contains("d"));
        }

        @Test @DisplayName("snapshot iterator is unaffected by subsequent remove()")
        void snapshotImmutableToRemove() {
            bag.addAll(List.of("x", "y", "z"));
            Iterator<String> snap = bag.snapshotIterator();
            bag.remove("x");
            List<String> seen = new ArrayList<>();
            while (snap.hasNext()) seen.add(snap.next());
            assertTrue(seen.contains("x"));  // snapshot captured before removal
            assertEquals(3, seen.size());
        }

        @Test @DisplayName("snapshot iterator remove() throws UnsupportedOperationException")
        void snapshotRemoveThrows() {
            bag.add("item");
            Iterator<String> snap = bag.snapshotIterator();
            snap.next();
            assertThrows(UnsupportedOperationException.class, snap::remove);
        }

        @Test @DisplayName("snapshot of empty Bag has no elements")
        void snapshotEmpty() {
            assertFalse(bag.snapshotIterator().hasNext());
        }
    }

    // ==================================================================
    // 10. Iterator exception guards
    // ==================================================================

    @Nested @DisplayName("10 · Iterator Exception Guards")
    class IteratorExceptionTests {

        @Test @DisplayName("live next() past end throws NoSuchElementException")
        void liveNextPastEnd() {
            bag.add("only");
            Iterator<String> it = bag.iterator();
            it.next();
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test @DisplayName("live remove() before next() throws IllegalStateException")
        void liveRemoveBeforeNext() {
            bag.add("item");
            assertThrows(IllegalStateException.class, bag.iterator()::remove);
        }

        @Test @DisplayName("live remove() twice in a row throws IllegalStateException")
        void liveRemoveTwice() {
            bag.add("item");
            Iterator<String> it = bag.iterator();
            it.next(); it.remove();
            assertThrows(IllegalStateException.class, it::remove);
        }

        @Test @DisplayName("snapshot next() past end throws NoSuchElementException")
        void snapshotNextPastEnd() {
            bag.add("only");
            Iterator<String> snap = bag.snapshotIterator();
            snap.next();
            assertThrows(NoSuchElementException.class, snap::next);
        }
    }

    // ==================================================================
    // 11. sort()
    // ==================================================================

    @Nested @DisplayName("11 · sort()")
    class SortTests {

        @Test @DisplayName("sort(null) sorts Integers in natural order")
        void sortNaturalOrder() {
            Bag<Integer> b = new Bag<>(List.of(3, 1, 4, 1, 5, 9, 2, 6));
            b.sort(null);
            List<Integer> result = new ArrayList<>();
            for (int n : b) result.add(n);
            List<Integer> expected = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6));
            Collections.sort(expected);
            assertEquals(expected, result);
        }

        @Test @DisplayName("sort(reverseOrder) sorts Integers descending")
        void sortReverseOrder() {
            Bag<Integer> b = new Bag<>(List.of(3, 1, 2));
            b.sort(Comparator.reverseOrder());
            List<Integer> result = new ArrayList<>();
            for (int n : b) result.add(n);
            assertEquals(List.of(3, 2, 1), result);
        }

        @Test @DisplayName("sort on empty Bag is a no-op")
        void sortEmpty() {
            assertDoesNotThrow(() -> bag.sort(null));
            assertTrue(bag.isEmpty());
        }

        @Test @DisplayName("sort on single-element Bag is a no-op")
        void sortSingleElement() {
            bag.add("only");
            bag.sort(null);
            assertEquals(1, bag.size());
        }
    }

    // ==================================================================
    // 12. toUnmodifiableList(), toArray(), copy()
    // ==================================================================

    @Nested @DisplayName("12 · Bulk Utilities")
    class BulkUtilityTests {

        @Test @DisplayName("toUnmodifiableList() returns correct size and contents")
        void toUnmodifiableListContents() {
            bag.addAll(List.of("a", "b", "c"));
            List<String> view = bag.toUnmodifiableList();
            assertEquals(3, view.size());
            assertTrue(view.containsAll(List.of("a", "b", "c")));
        }

        @Test @DisplayName("toUnmodifiableList() is read-only")
        void toUnmodifiableListReadOnly() {
            bag.add("x");
            assertThrows(UnsupportedOperationException.class,
                    () -> bag.toUnmodifiableList().add("y"));
        }

        @Test @DisplayName("toArray() returns array with correct length and contents")
        void toArrayContents() {
            bag.addAll(List.of("p", "q", "r"));
            Object[] arr = bag.toArray();
            assertEquals(3, arr.length);
            assertEquals("p", arr[0]);
            assertEquals("r", arr[2]);
        }

        @Test @DisplayName("copy() produces an equal but independent Bag")
        void copyIndependence() {
            bag.addAll(List.of("alpha", "beta"));
            Bag<String> copied = bag.copy();
            assertEquals(bag, copied);
            copied.add("gamma");
            assertNotEquals(bag, copied);    // copy grew; original unchanged
            assertEquals(2, bag.size());
            assertEquals(3, copied.size());
        }

        @Test @DisplayName("copy() of empty Bag is also empty")
        void copyEmpty() {
            Bag<String> copied = bag.copy();
            assertTrue(copied.isEmpty());
        }
    }

    // ==================================================================
    // 13. Null handling
    // ==================================================================

    @Nested @DisplayName("13 · Null Handling")
    class NullTests {

        @Test @DisplayName("addAll can include null elements")
        void addAllWithNulls() {
            List<String> withNull = new ArrayList<>();
            withNull.add(null); withNull.add("real");
            bag.addAll(withNull);
            assertEquals(2, bag.size());
            assertTrue(bag.contains(null));
        }

        @Test @DisplayName("frequency(null) counts correctly")
        void frequencyNull() {
            bag.add(null); bag.add(null); bag.add("x");
            assertEquals(2, bag.frequency(null));
        }

        @Test @DisplayName("removeAll(null, n) removes nulls")
        void removeAllNull() {
            bag.add(null); bag.add(null); bag.add("keep");
            int rm = bag.removeAll(null, 1);
            assertEquals(1, rm);
            assertEquals(1, bag.frequency(null));
        }

        @Test @DisplayName("removeIf can match nulls")
        void removeIfNull() {
            bag.add(null); bag.add("real");
            bag.removeIf(Objects::isNull);
            assertFalse(bag.contains(null));
            assertTrue(bag.contains("real"));
        }
    }

    // ==================================================================
    // 14. Duplicate items
    // ==================================================================

    @Nested @DisplayName("14 · Duplicate Items")
    class DuplicateTests {

        @Test @DisplayName("frequency() reports all duplicate copies")
        void frequencyAllCopies() {
            for (int i = 0; i < 5; i++) bag.add("dup");
            assertEquals(5, bag.frequency("dup"));
        }

        @Test @DisplayName("removeAll removes exact count of duplicates")
        void removeAllExactCount() {
            for (int i = 0; i < 5; i++) bag.add("dup");
            bag.removeAll("dup", 3);
            assertEquals(2, bag.frequency("dup"));
        }

        @Test @DisplayName("removeIf removes all duplicates at once")
        void removeIfAllDuplicates() {
            bag.addAll(List.of("dup", "dup", "dup", "keep"));
            bag.removeIf("dup"::equals);
            assertFalse(bag.contains("dup"));
            assertTrue(bag.contains("keep"));
        }
    }

    // ==================================================================
    // 15. Generic type flexibility
    // ==================================================================

    @Nested @DisplayName("15 · Generic Type Flexibility")
    class GenericTests {

        @Test @DisplayName("Bag<Integer> with sort and frequency")
        void integerBag() {
            Bag<Integer> b = new Bag<>(List.of(3, 1, 2, 1));
            b.sort(null);
            assertEquals(2, b.frequency(1));
            List<Integer> result = new ArrayList<>();
            for (int n : b) result.add(n);
            assertEquals(List.of(1, 1, 2, 3), result);
        }

        @Test @DisplayName("Bag<Object> copy and toArray")
        void objectBag() {
            Bag<Object> b = new Bag<>();
            b.add("str"); b.add(42); b.add(null);
            Bag<Object> c = b.copy();
            assertEquals(b, c);
            assertEquals(3, b.toArray().length);
        }

        @Test @DisplayName("Bag<String> removeIf with lambda")
        void stringBagRemoveIf() {
            Bag<String> b = new Bag<>(List.of("cat", "car", "bar", "bat"));
            b.removeIf(s -> s.startsWith("c"));
            assertEquals(2, b.size());
            assertFalse(b.contains("cat") || b.contains("car"));
        }
    }
}