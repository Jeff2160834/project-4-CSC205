package com.example.iterable;


import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for {@link Bag}.
 *
 * Test groups:
 *  1.  Empty-Bag operations
 *  2.  add() operations
 *  3.  remove() operations
 *  4.  contains() operations
 *  5.  Iterator functionality
 *  6.  Edge cases (null handling, duplicates, non-existent removes, etc.)
 */
@DisplayName("Bag<E> – Full Test Suite")
class BagTest {

    // ------------------------------------------------------------------
    // Shared fixture
    // ------------------------------------------------------------------

    private Bag<String> bag;

    @BeforeEach
    void setUp() {
        bag = new Bag<>();
    }

    // ==================================================================
    // 1. EMPTY BAG OPERATIONS
    // ==================================================================

    @Nested
    @DisplayName("1 · Empty Bag Operations")
    class EmptyBagTests {

        @Test
        @DisplayName("new Bag is empty")
        void newBagIsEmpty() {
            assertTrue(bag.isEmpty());
        }

        @Test
        @DisplayName("new Bag has size 0")
        void newBagHasSizeZero() {
            assertEquals(0, bag.size());
        }

        @Test
        @DisplayName("contains() on empty Bag returns false")
        void containsOnEmptyReturnsFalse() {
            assertFalse(bag.contains("anything"));
        }

        @Test
        @DisplayName("remove() on empty Bag returns false")
        void removeOnEmptyReturnsFalse() {
            assertFalse(bag.remove("ghost"));
        }

        @Test
        @DisplayName("iterator on empty Bag has no next element")
        void iteratorOnEmptyHasNoNext() {
            assertFalse(bag.iterator().hasNext());
        }

        @Test
        @DisplayName("iterator on empty Bag throws NoSuchElementException on next()")
        void iteratorOnEmptyThrowsOnNext() {
            Iterator<String> it = bag.iterator();
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        @DisplayName("custom initial-capacity constructor produces empty Bag")
        void capacityConstructorProducesEmptyBag() {
            Bag<Integer> intBag = new Bag<>(20);
            assertTrue(intBag.isEmpty());
            assertEquals(0, intBag.size());
        }

        @Test
        @DisplayName("negative initial capacity throws IllegalArgumentException")
        void negativeCapacityThrows() {
            assertThrows(IllegalArgumentException.class, () -> new Bag<>(-1));
        }
    }

    // ==================================================================
    // 2. ADD OPERATIONS
    // ==================================================================

    @Nested
    @DisplayName("2 · add() Operations")
    class AddTests {

        @Test
        @DisplayName("add() increases size by 1")
        void addIncreasesSizeByOne() {
            bag.add("alpha");
            assertEquals(1, bag.size());
        }

        @Test
        @DisplayName("add() makes Bag non-empty")
        void addMakesBagNonEmpty() {
            bag.add("beta");
            assertFalse(bag.isEmpty());
        }

        @Test
        @DisplayName("multiple adds accumulate correct size")
        void multipleAddsAccumulateSize() {
            bag.add("a");
            bag.add("b");
            bag.add("c");
            assertEquals(3, bag.size());
        }

        @Test
        @DisplayName("adding many items scales correctly")
        void addManyItemsScales() {
            for (int i = 0; i < 1_000; i++) {
                bag.add("item-" + i);
            }
            assertEquals(1_000, bag.size());
        }

        @Test
        @DisplayName("add() accepts null")
        void addAcceptsNull() {
            assertDoesNotThrow(() -> bag.add(null));
            assertEquals(1, bag.size());
        }

        @Test
        @DisplayName("add() preserves insertion order (verified via iterator)")
        void addPreservesInsertionOrder() {
            bag.add("first");
            bag.add("second");
            bag.add("third");

            List<String> result = new ArrayList<>();
            for (String s : bag) result.add(s);

            assertEquals(List.of("first", "second", "third"), result);
        }

        @ParameterizedTest(name = "add generic type: {0}")
        @ValueSource(strings = {"hello", "world", "java", "generics"})
        @DisplayName("add() works for various String values")
        void addVariousStrings(String value) {
            bag.add(value);
            assertTrue(bag.contains(value));
        }
    }

    // ==================================================================
    // 3. REMOVE OPERATIONS
    // ==================================================================

    @Nested
    @DisplayName("3 · remove() Operations")
    class RemoveTests {

        @Test
        @DisplayName("remove() existing item returns true")
        void removeExistingReturnsTrue() {
            bag.add("target");
            assertTrue(bag.remove("target"));
        }

        @Test
        @DisplayName("remove() existing item decreases size")
        void removeDecreasesSize() {
            bag.add("x");
            bag.add("y");
            bag.remove("x");
            assertEquals(1, bag.size());
        }

        @Test
        @DisplayName("remove() makes Bag empty when last element removed")
        void removeLastElementMakesBagEmpty() {
            bag.add("solo");
            bag.remove("solo");
            assertTrue(bag.isEmpty());
        }

        @Test
        @DisplayName("remove() non-existent item returns false and does not change size")
        void removeNonExistentReturnsFalse() {
            bag.add("present");
            assertFalse(bag.remove("absent"));
            assertEquals(1, bag.size());
        }

        @Test
        @DisplayName("remove() removes only the first occurrence of a duplicate")
        void removeOnlyFirstOccurrence() {
            bag.add("dup");
            bag.add("dup");
            bag.remove("dup");
            assertEquals(1, bag.size());
            assertTrue(bag.contains("dup"));
        }

        @Test
        @DisplayName("remove(null) removes the first null")
        void removeNullRemovesFirstNull() {
            bag.add(null);
            bag.add("after-null");
            assertTrue(bag.remove(null));
            assertEquals(1, bag.size());
            assertTrue(bag.contains("after-null"));
        }

        @Test
        @DisplayName("sequential removes empty the Bag")
        void sequentialRemovesEmptyBag() {
            bag.add("a");
            bag.add("b");
            bag.add("c");
            bag.remove("a");
            bag.remove("b");
            bag.remove("c");
            assertTrue(bag.isEmpty());
        }
    }

    // ==================================================================
    // 4. CONTAINS OPERATIONS
    // ==================================================================

    @Nested
    @DisplayName("4 · contains() Operations")
    class ContainsTests {

        @Test
        @DisplayName("contains() returns true for an added item")
        void containsAddedItem() {
            bag.add("present");
            assertTrue(bag.contains("present"));
        }

        @Test
        @DisplayName("contains() returns false for an item never added")
        void containsAbsentItem() {
            bag.add("present");
            assertFalse(bag.contains("absent"));
        }

        @Test
        @DisplayName("contains() returns false after the only occurrence is removed")
        void containsFalseAfterRemove() {
            bag.add("temp");
            bag.remove("temp");
            assertFalse(bag.contains("temp"));
        }

        @Test
        @DisplayName("contains(null) returns true when null was added")
        void containsNullWhenNullAdded() {
            bag.add(null);
            assertTrue(bag.contains(null));
        }

        @Test
        @DisplayName("contains(null) returns false when null was never added")
        void containsNullWhenNotAdded() {
            bag.add("notNull");
            assertFalse(bag.contains(null));
        }

        @Test
        @DisplayName("contains() is true for each of multiple distinct items")
        void containsEachDistinctItem() {
            List<String> values = List.of("alpha", "beta", "gamma");
            values.forEach(bag::add);
            values.forEach(v -> assertTrue(bag.contains(v),
                    "Expected Bag to contain: " + v));
        }

        @Test
        @DisplayName("contains() is true for duplicate items after one is removed")
        void containsTrueForDuplicateAfterOneRemoved() {
            bag.add("dup");
            bag.add("dup");
            bag.remove("dup");
            assertTrue(bag.contains("dup"),
                    "Second duplicate should still be present");
        }
    }

    // ==================================================================
    // 5. ITERATOR FUNCTIONALITY
    // ==================================================================

    @Nested
    @DisplayName("5 · Iterator Functionality")
    class IteratorTests {

        @Test
        @DisplayName("iterator visits every element exactly once")
        void iteratorVisitsEveryElement() {
            bag.add("one");
            bag.add("two");
            bag.add("three");

            List<String> visited = new ArrayList<>();
            for (String s : bag) visited.add(s);

            assertEquals(3, visited.size());
            assertTrue(visited.containsAll(List.of("one", "two", "three")));
        }

        @Test
        @DisplayName("iterator traverses elements in insertion order")
        void iteratorPreservesInsertionOrder() {
            bag.add("first");
            bag.add("second");
            bag.add("third");

            List<String> result = new ArrayList<>();
            bag.iterator().forEachRemaining(result::add);

            assertEquals(List.of("first", "second", "third"), result);
        }

        @Test
        @DisplayName("hasNext() is idempotent – repeated calls do not advance")
        void hasNextIsIdempotent() {
            bag.add("only");
            Iterator<String> it = bag.iterator();
            assertTrue(it.hasNext());
            assertTrue(it.hasNext()); // second call must still be true
            it.next();
            assertFalse(it.hasNext());
            assertFalse(it.hasNext()); // still false
        }

        @Test
        @DisplayName("iterator.remove() removes current element")
        void iteratorRemoveDeletesCurrentElement() {
            bag.add("keep");
            bag.add("delete");
            bag.add("keep-too");

            Iterator<String> it = bag.iterator();
            while (it.hasNext()) {
                if ("delete".equals(it.next())) {
                    it.remove();
                }
            }

            assertEquals(2, bag.size());
            assertFalse(bag.contains("delete"));
        }

        @Test
        @DisplayName("iterator.remove() before next() throws IllegalStateException")
        void iteratorRemoveBeforeNextThrows() {
            bag.add("item");
            Iterator<String> it = bag.iterator();
            assertThrows(IllegalStateException.class, it::remove);
        }

        @Test
        @DisplayName("iterator.remove() twice in a row throws IllegalStateException")
        void iteratorRemoveTwiceInRowThrows() {
            bag.add("item");
            Iterator<String> it = bag.iterator();
            it.next();
            it.remove();
            assertThrows(IllegalStateException.class, it::remove);
        }

        @Test
        @DisplayName("iterator.next() past end throws NoSuchElementException")
        void iteratorNextPastEndThrows() {
            bag.add("item");
            Iterator<String> it = bag.iterator();
            it.next();
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        @DisplayName("for-each loop on single-element Bag iterates once")
        void forEachOnSingleElement() {
            bag.add("solo");
            int count = 0;
            for (String ignored : bag) count++;
            assertEquals(1, count);
        }

        @Test
        @DisplayName("multiple independent iterators do not interfere")
        void multipleIndependentIterators() {
            bag.add("A");
            bag.add("B");

            Iterator<String> it1 = bag.iterator();
            Iterator<String> it2 = bag.iterator();

            assertEquals("A", it1.next());
            assertEquals("A", it2.next()); // it2 should start fresh
            assertEquals("B", it1.next());
            assertEquals("B", it2.next());
        }

        @Test
        @DisplayName("iterator works correctly with generic Integer Bag")
        void iteratorWorksWithIntegerBag() {
            Bag<Integer> intBag = new Bag<>();
            for (int i = 1; i <= 5; i++) intBag.add(i);

            int sum = 0;
            for (int n : intBag) sum += n;

            assertEquals(15, sum); // 1+2+3+4+5
        }
    }

    // ==================================================================
    // 6. EDGE CASES
    // ==================================================================

    @Nested
    @DisplayName("6 · Edge Cases")
    class EdgeCaseTests {

        // ---- Null handling -----------------------------------------------

        @Test
        @DisplayName("add(null) then contains(null) returns true")
        void addNullContainsNull() {
            bag.add(null);
            assertTrue(bag.contains(null));
        }

        @Test
        @DisplayName("add multiple nulls; size reflects all")
        void addMultipleNullsTrackedBySize() {
            bag.add(null);
            bag.add(null);
            assertEquals(2, bag.size());
        }

        @Test
        @DisplayName("remove(null) removes only one null when multiple nulls present")
        void removeOnlyOneNullWhenMultiplePresent() {
            bag.add(null);
            bag.add(null);
            bag.remove(null);
            assertEquals(1, bag.size());
            assertTrue(bag.contains(null));
        }

        @Test
        @DisplayName("iterator correctly visits null elements")
        void iteratorVisitsNullElements() {
            bag.add("before");
            bag.add(null);
            bag.add("after");

            List<String> result = new ArrayList<>();
            for (String s : bag) result.add(s);

            assertEquals(3, result.size());
            assertNull(result.get(1));
        }

        // ---- Removing non-existent items ----------------------------------

        @Test
        @DisplayName("remove() of item never added returns false")
        void removeNeverAddedReturnsFalse() {
            assertFalse(bag.remove("ghost"));
        }

        @Test
        @DisplayName("remove() after all occurrences gone returns false")
        void removeAfterAllGoneReturnsFalse() {
            bag.add("once");
            bag.remove("once");
            assertFalse(bag.remove("once"));
        }

        @Test
        @DisplayName("remove() does not alter size when item is absent")
        void removeAbsentDoesNotAlterSize() {
            bag.add("existing");
            bag.remove("missing");
            assertEquals(1, bag.size());
        }

        // ---- Duplicate items ---------------------------------------------

        @Test
        @DisplayName("add duplicate preserves both copies")
        void addDuplicatePreservesBothCopies() {
            bag.add("dup");
            bag.add("dup");
            assertEquals(2, bag.size());
        }

        @Test
        @DisplayName("contains() returns true while any duplicate remains")
        void containsTrueWhileAnyDuplicateRemains() {
            bag.add("dup");
            bag.add("dup");
            bag.remove("dup");
            assertTrue(bag.contains("dup"));
        }

        @Test
        @DisplayName("iterator counts duplicate occurrences separately")
        void iteratorCountsDuplicatesSeparately() {
            bag.add("dup");
            bag.add("dup");
            bag.add("dup");

            long count = 0;
            for (String s : bag) {
                if ("dup".equals(s)) count++;
            }
            assertEquals(3, count);
        }

        // ---- Mixed generic type ------------------------------------------

        @Test
        @DisplayName("Bag<Object> can hold items of mixed runtime types")
        void bagOfObjectHoldsMixedTypes() {
            Bag<Object> mixed = new Bag<>();
            mixed.add("string");
            mixed.add(42);
            mixed.add(3.14);
            mixed.add(null);

            assertEquals(4, mixed.size());
            assertTrue(mixed.contains("string"));
            assertTrue(mixed.contains(42));
            assertTrue(mixed.contains(null));
        }

        // ---- toString / equals / hashCode --------------------------------

        @Test
        @DisplayName("toString() includes all elements")
        void toStringIncludesAllElements() {
            bag.add("hello");
            bag.add("world");
            String str = bag.toString();
            assertTrue(str.contains("hello") && str.contains("world"));
        }

        @Test
        @DisplayName("two Bags with same elements in same order are equal")
        void equalBagsAreEqual() {
            Bag<String> other = new Bag<>();
            bag.add("x");
            other.add("x");
            assertEquals(bag, other);
        }

        @Test
        @DisplayName("Bag is equal to itself (reflexive)")
        void bagEqualsItself() {
            bag.add("a");
            assertEquals(bag, bag);
        }

        @Test
        @DisplayName("two Bags with different elements are not equal")
        void unequalBagsAreNotEqual() {
            Bag<String> other = new Bag<>();
            bag.add("a");
            other.add("b");
            assertNotEquals(bag, other);
        }
    }
}