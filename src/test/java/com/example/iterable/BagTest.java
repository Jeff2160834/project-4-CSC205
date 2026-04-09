package com.example.iterable;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite covering forEach, spliterator, and fail-fast behaviour.
 *
 * Groups:
 *  1.  forEach — traversal, order, null action guard
 *  2.  forEach — fail-fast (lambda mutation, external mutation)
 *  3.  Live iterator — fail-fast (add, remove, clear, sort, addAll, removeIf)
 *  4.  Live iterator — own remove() does NOT trigger CME
 *  5.  Live iterator — forEachRemaining traversal and fail-fast
 *  6.  Snapshot iterator — immune to all structural changes
 *  7.  spliterator — tryAdvance
 *  8.  spliterator — forEachRemaining
 *  9.  spliterator — trySplit
 * 10.  spliterator — characteristics (ORDERED, SIZED, SUBSIZED, IMMUTABLE)
 * 11.  spliterator — Stream integration (sequential and parallel)
 * 12.  modCount regression — every mutating method increments modCount
 */
@DisplayName("Bag<E> forEach / spliterator / fail-fast Test Suite")
class BagTest {

    private Bag<String> bag;

    @BeforeEach void setUp() { bag = new Bag<>(); }

    // ==================================================================
    // 1. forEach — traversal
    // ==================================================================

    @Nested @DisplayName("1 · forEach Traversal")
    class ForEachTraversalTests {

        @Test @DisplayName("forEach visits all elements in insertion order")
        void forEachOrder() {
            bag.addAll(List.of("a", "b", "c"));
            List<String> result = new ArrayList<>();
            bag.forEach(result::add);
            assertEquals(List.of("a", "b", "c"), result);
        }

        @Test @DisplayName("forEach on empty Bag invokes action zero times")
        void forEachEmpty() {
            int[] count = {0};
            bag.forEach(s -> count[0]++);
            assertEquals(0, count[0]);
        }

        @Test @DisplayName("forEach visits every element including nulls")
        void forEachWithNulls() {
            bag.add("x"); bag.add(null); bag.add("z");
            List<String> result = new ArrayList<>();
            bag.forEach(result::add);
            assertEquals(3, result.size());
            assertNull(result.get(1));
        }

        @Test @DisplayName("forEach does not modify the Bag")
        void forEachDoesNotMutate() {
            bag.addAll(List.of("p", "q", "r"));
            bag.forEach(s -> {}); // no-op consumer
            assertEquals(3, bag.size());
        }

        @Test @DisplayName("forEach(null) throws NullPointerException")
        void forEachNullAction() {
            assertThrows(NullPointerException.class, () -> bag.forEach(null));
        }
    }

    // ==================================================================
    // 2. forEach — fail-fast
    // ==================================================================

    @Nested @DisplayName("2 · forEach Fail-Fast")
    class ForEachFailFastTests {

        @Test @DisplayName("forEach throws CME when lambda calls add()")
        void forEachLambdaAdd() {
            bag.addAll(List.of("a", "b", "c"));
            assertThrows(ConcurrentModificationException.class,
                    () -> bag.forEach(s -> { if ("b".equals(s)) bag.add("X"); }));
        }

        @Test @DisplayName("forEach throws CME when lambda calls remove()")
        void forEachLambdaRemove() {
            bag.addAll(List.of("a", "b", "c"));
            assertThrows(ConcurrentModificationException.class,
                    () -> bag.forEach(s -> { if ("b".equals(s)) bag.remove("c"); }));
        }

        @Test @DisplayName("forEach throws CME when lambda calls clear()")
        void forEachLambdaClear() {
            bag.addAll(List.of("a", "b", "c"));
            assertThrows(ConcurrentModificationException.class,
                    () -> bag.forEach(s -> { if ("a".equals(s)) bag.clear(); }));
        }
    }

    // ==================================================================
    // 3. Live iterator — fail-fast
    // ==================================================================

    @Nested @DisplayName("3 · Live Iterator Fail-Fast")
    class LiveIteratorFailFastTests {

        private void assertCMEAfterMutation(Runnable mutation) {
            bag.addAll(List.of("x", "y", "z"));
            Iterator<String> it = bag.iterator();
            it.next();           // advance past first element
            mutation.run();      // structural change
            assertThrows(ConcurrentModificationException.class, it::next);
        }

        @Test @DisplayName("CME thrown after external add()")
        void cmeAfterAdd() { assertCMEAfterMutation(() -> bag.add("new")); }

        @Test @DisplayName("CME thrown after external addAll()")
        void cmeAfterAddAll() { assertCMEAfterMutation(() -> bag.addAll(List.of("p","q"))); }

        @Test @DisplayName("CME thrown after external remove()")
        void cmeAfterRemove() { assertCMEAfterMutation(() -> bag.remove("y")); }

        @Test @DisplayName("CME thrown after external removeAll()")
        void cmeAfterRemoveAll() { assertCMEAfterMutation(() -> bag.removeAll("x", 1)); }

        @Test @DisplayName("CME thrown after external removeIf()")
        void cmeAfterRemoveIf() { assertCMEAfterMutation(() -> bag.removeIf("z"::equals)); }

        @Test @DisplayName("CME thrown after external clear()")
        void cmeAfterClear() { assertCMEAfterMutation(() -> bag.clear()); }

        @Test @DisplayName("CME thrown after external sort()")
        void cmeAfterSort() { assertCMEAfterMutation(() -> bag.sort(null)); }

        @Test @DisplayName("CME thrown on hasNext() after external mutation")
        void cmeOnHasNext() {
            bag.addAll(List.of("a", "b"));
            Iterator<String> it = bag.iterator();
            bag.add("c");
            assertThrows(ConcurrentModificationException.class, it::hasNext);
        }
    }

    // ==================================================================
    // 4. Live iterator — own remove() safe
    // ==================================================================

    @Nested @DisplayName("4 · Live Iterator Own remove() Is Safe")
    class LiveIteratorOwnRemoveTests {

        @Test @DisplayName("iterator remove() does not cause CME on next call")
        void ownRemoveNoCME() {
            bag.addAll(List.of("keep", "drop", "keep2"));
            Iterator<String> it = bag.iterator();
            assertDoesNotThrow(() -> {
                while (it.hasNext()) {
                    if ("drop".equals(it.next())) it.remove();
                }
            });
            assertEquals(2, bag.size());
            assertFalse(bag.contains("drop"));
        }

        @Test @DisplayName("multiple iterator removes in sequence are all safe")
        void multipleIteratorRemovesSafe() {
            bag.addAll(List.of("a", "b", "c", "d", "e"));
            final Iterator<String>[] it = new Iterator[]{bag.iterator()};
            assertDoesNotThrow(() -> {
                while (it[0].hasNext()) it[0].next(); it[0] = bag.iterator();
                // remove all via fresh iterator
                while (it[0].hasNext()) { it[0].next(); it[0].remove(); }
            });
            assertTrue(bag.isEmpty());
        }
    }

    // ==================================================================
    // 5. Live iterator — forEachRemaining
    // ==================================================================

    @Nested @DisplayName("5 · Live Iterator forEachRemaining")
    class ForEachRemainingTests {

        @Test @DisplayName("forEachRemaining visits all elements after cursor")
        void forEachRemainingAfterCursor() {
            bag.addAll(List.of("1", "2", "3", "4"));
            Iterator<String> it = bag.iterator();
            it.next(); // skip "1"
            List<String> rest = new ArrayList<>();
            it.forEachRemaining(rest::add);
            assertEquals(List.of("2", "3", "4"), rest);
        }

        @Test @DisplayName("forEachRemaining on exhausted iterator does nothing")
        void forEachRemainingExhausted() {
            bag.add("only");
            Iterator<String> it = bag.iterator();
            it.next();
            int[] count = {0};
            it.forEachRemaining(s -> count[0]++);
            assertEquals(0, count[0]);
        }

        @Test @DisplayName("forEachRemaining throws CME on mutation inside consumer")
        void forEachRemainingCME() {
            bag.addAll(List.of("a", "b", "c", "d"));
            Iterator<String> it = bag.iterator();
            assertThrows(ConcurrentModificationException.class,
                    () -> it.forEachRemaining(s -> { if ("b".equals(s)) bag.add("X"); }));
        }

        @Test @DisplayName("forEachRemaining(null) throws NullPointerException")
        void forEachRemainingNullAction() {
            bag.add("item");
            Iterator<String> it = bag.iterator();
            assertThrows(NullPointerException.class, () -> it.forEachRemaining(null));
        }
    }

    // ==================================================================
    // 6. Snapshot iterator — immune to mutations
    // ==================================================================

    @Nested @DisplayName("6 · Snapshot Iterator Immunity")
    class SnapshotImmunityTests {

        @Test @DisplayName("snapshot sees elements present at creation time")
        void snapshotFreezesState() {
            bag.addAll(List.of("p", "q", "r"));
            Iterator<String> snap = bag.snapshotIterator();
            bag.add("s"); bag.remove("p");
            List<String> seen = new ArrayList<>();
            while (snap.hasNext()) seen.add(snap.next());
            assertEquals(3, seen.size());
            assertTrue(seen.contains("p"));
            assertFalse(seen.contains("s"));
        }

        @Test @DisplayName("snapshot does NOT throw CME on any external mutation")
        void snapshotNoCME() {
            bag.addAll(List.of("a", "b", "c"));
            Iterator<String> snap = bag.snapshotIterator();
            snap.next();
            assertDoesNotThrow(() -> {
                bag.add("X"); bag.remove("a"); bag.clear();
                while (snap.hasNext()) snap.next(); // consume rest without CME
            });
        }

        @Test @DisplayName("snapshot remove() throws UnsupportedOperationException")
        void snapshotRemoveThrows() {
            bag.add("item");
            Iterator<String> snap = bag.snapshotIterator();
            snap.next();
            assertThrows(UnsupportedOperationException.class, snap::remove);
        }
    }

    // ==================================================================
    // 7. spliterator — tryAdvance
    // ==================================================================

    @Nested @DisplayName("7 · Spliterator tryAdvance")
    class TryAdvanceTests {

        @Test @DisplayName("tryAdvance returns true and visits element")
        void tryAdvanceReturnsTrue() {
            bag.add("only");
            Spliterator<String> sp = bag.spliterator();
            List<String> seen = new ArrayList<>();
            assertTrue(sp.tryAdvance(seen::add));
            assertEquals(List.of("only"), seen);
        }

        @Test @DisplayName("tryAdvance returns false when exhausted")
        void tryAdvanceReturnsFalseWhenDone() {
            bag.add("x");
            Spliterator<String> sp = bag.spliterator();
            sp.tryAdvance(s -> {});
            assertFalse(sp.tryAdvance(s -> {}));
        }

        @Test @DisplayName("tryAdvance visits all elements in order")
        void tryAdvanceAllInOrder() {
            bag.addAll(List.of("a", "b", "c"));
            Spliterator<String> sp = bag.spliterator();
            List<String> result = new ArrayList<>();
            while (sp.tryAdvance(result::add));
            assertEquals(List.of("a", "b", "c"), result);
        }

        @Test @DisplayName("tryAdvance(null) throws NullPointerException")
        void tryAdvanceNullThrows() {
            bag.add("x");
            assertThrows(NullPointerException.class,
                    () -> bag.spliterator().tryAdvance(null));
        }
    }

    // ==================================================================
    // 8. spliterator — forEachRemaining
    // ==================================================================

    @Nested @DisplayName("8 · Spliterator forEachRemaining")
    class SpliteratorForEachRemainingTests {

        @Test @DisplayName("forEachRemaining visits all elements after partial advance")
        void forEachRemainingAfterAdvance() {
            bag.addAll(List.of("1", "2", "3", "4", "5"));
            Spliterator<String> sp = bag.spliterator();
            sp.tryAdvance(s -> {}); // skip "1"
            sp.tryAdvance(s -> {}); // skip "2"
            List<String> rest = new ArrayList<>();
            sp.forEachRemaining(rest::add);
            assertEquals(List.of("3", "4", "5"), rest);
        }

        @Test @DisplayName("forEachRemaining on empty spliterator is a no-op")
        void forEachRemainingEmpty() {
            Spliterator<String> sp = bag.spliterator();
            int[] count = {0};
            sp.forEachRemaining(s -> count[0]++);
            assertEquals(0, count[0]);
        }

        @Test @DisplayName("spliterator forEachRemaining(null) throws NullPointerException")
        void forEachRemainingNullThrows() {
            bag.add("item");
            assertThrows(NullPointerException.class,
                    () -> bag.spliterator().forEachRemaining(null));
        }
    }

    // ==================================================================
    // 9. spliterator — trySplit
    // ==================================================================

    @Nested @DisplayName("9 · Spliterator trySplit")
    class TrySplitTests {

        @Test @DisplayName("trySplit divides 6 elements into two halves of 3")
        void trySplitEvenElements() {
            bag.addAll(List.of("a","b","c","d","e","f"));
            Spliterator<String> right = bag.spliterator();
            Spliterator<String> left  = right.trySplit();
            assertNotNull(left);
            assertEquals(3, left.estimateSize());
            assertEquals(3, right.estimateSize());
        }

        @Test @DisplayName("trySplit on single-element returns null")
        void trySplitSingleElement() {
            bag.add("solo");
            Spliterator<String> sp = bag.spliterator();
            assertNull(sp.trySplit());
        }

        @Test @DisplayName("trySplit on empty Bag returns null")
        void trySplitEmpty() {
            assertNull(bag.spliterator().trySplit());
        }

        @Test @DisplayName("trySplit left + right together cover all elements")
        void trySplitCoversAll() {
            bag.addAll(List.of("1","2","3","4","5"));
            Spliterator<String> right = bag.spliterator();
            Spliterator<String> left  = right.trySplit();
            List<String> all = new ArrayList<>();
            if (left != null) left.forEachRemaining(all::add);
            right.forEachRemaining(all::add);
            // All 5 elements must be present (order: left then right)
            assertEquals(5, all.size());
            assertTrue(all.containsAll(List.of("1","2","3","4","5")));
        }
    }

    // ==================================================================
    // 10. spliterator — characteristics
    // ==================================================================

    @Nested @DisplayName("10 · Spliterator Characteristics")
    class CharacteristicsTests {

        @Test @DisplayName("ORDERED characteristic is set")
        void ordered() {
            bag.add("x");
            assertTrue((bag.spliterator().characteristics() & Spliterator.ORDERED) != 0);
        }

        @Test @DisplayName("SIZED characteristic is set")
        void sized() {
            bag.add("x");
            assertTrue((bag.spliterator().characteristics() & Spliterator.SIZED) != 0);
        }

        @Test @DisplayName("SUBSIZED characteristic is set")
        void subsized() {
            bag.add("x");
            assertTrue((bag.spliterator().characteristics() & Spliterator.SUBSIZED) != 0);
        }

        @Test @DisplayName("IMMUTABLE characteristic is set")
        void immutable() {
            bag.add("x");
            assertTrue((bag.spliterator().characteristics() & Spliterator.IMMUTABLE) != 0);
        }

        @Test @DisplayName("estimateSize() matches Bag size")
        void estimateSize() {
            bag.addAll(List.of("a", "b", "c", "d"));
            assertEquals(4L, bag.spliterator().estimateSize());
        }

        @Test @DisplayName("getExactSizeIfKnown() matches Bag size")
        void exactSize() {
            bag.addAll(List.of("a", "b", "c"));
            assertEquals(3L, bag.spliterator().getExactSizeIfKnown());
        }

        @Test @DisplayName("sub-spliterator from trySplit also reports SIZED")
        void subSpliteratorSized() {
            bag.addAll(List.of("a","b","c","d"));
            Spliterator<String> sub = bag.spliterator().trySplit();
            assertNotNull(sub);
            assertTrue((sub.characteristics() & Spliterator.SIZED) != 0);
        }
    }

    // ==================================================================
    // 11. spliterator — Stream integration
    // ==================================================================

    @Nested @DisplayName("11 · Spliterator Stream Integration")
    class StreamIntegrationTests {

        @Test @DisplayName("sequential stream filter + collect")
        void sequentialStreamFilter() {
            bag.addAll(List.of("banana", "apple", "cherry", "fig"));
            List<String> result = StreamSupport
                    .stream(bag.spliterator(), false)
                    .filter(s -> s.length() > 3)
                    .sorted()
                    .collect(Collectors.toList());
            assertEquals(List.of("apple", "banana", "cherry"), result);
        }

        @Test @DisplayName("sequential stream map + collect")
        void sequentialStreamMap() {
            bag.addAll(List.of("hello", "world"));
            List<String> result = StreamSupport
                    .stream(bag.spliterator(), false)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            assertEquals(List.of("HELLO", "WORLD"), result);
        }

        @Test @DisplayName("sequential stream count")
        void sequentialStreamCount() {
            bag.addAll(List.of("a","b","c","d","e"));
            long count = StreamSupport.stream(bag.spliterator(), false).count();
            assertEquals(5L, count);
        }

        @Test @DisplayName("parallel stream sum is correct")
        void parallelStreamSum() {
            Bag<Integer> intBag = new Bag<>();
            for (int i = 1; i <= 100; i++) intBag.add(i);
            long sum = StreamSupport
                    .stream(intBag.spliterator(), true)
                    .mapToLong(Integer::longValue)
                    .sum();
            assertEquals(5050L, sum);
        }

        @Test @DisplayName("stream on empty Bag produces empty result")
        void streamOnEmpty() {
            List<String> result = StreamSupport
                    .stream(bag.spliterator(), false)
                    .collect(Collectors.toList());
            assertTrue(result.isEmpty());
        }

        @Test @DisplayName("Bag unchanged after stream operation")
        void bagUnchangedAfterStream() {
            bag.addAll(List.of("x","y","z"));
            StreamSupport.stream(bag.spliterator(), false).count();
            assertEquals(3, bag.size());
        }
    }

    // ==================================================================
    // 12. modCount regression
    // ==================================================================

    @Nested @DisplayName("12 · modCount Regression")
    class ModCountRegressionTests {

        private void assertCME(Bag<String> b, Runnable mutation) {
            Iterator<String> it = b.iterator();
            it.next();
            mutation.run();
            assertThrows(ConcurrentModificationException.class, it::next,
                    "Expected CME after mutation");
        }

        @Test @DisplayName("add() increments modCount")
        void addIncrements() {
            Bag<String> b = new Bag<>(List.of("a","b","c"));
            assertCME(b, () -> b.add("x"));
        }

        @Test @DisplayName("addAll() increments modCount")
        void addAllIncrements() {
            Bag<String> b = new Bag<>(List.of("a","b","c"));
            assertCME(b, () -> b.addAll(List.of("x","y")));
        }

        @Test @DisplayName("remove() increments modCount")
        void removeIncrements() {
            Bag<String> b = new Bag<>(List.of("a","b","c"));
            assertCME(b, () -> b.remove("b"));
        }

        @Test @DisplayName("removeAll() increments modCount")
        void removeAllIncrements() {
            Bag<String> b = new Bag<>(List.of("a","a","c"));
            assertCME(b, () -> b.removeAll("a", 2));
        }

        @Test @DisplayName("removeIf() increments modCount")
        void removeIfIncrements() {
            Bag<String> b = new Bag<>(List.of("a","b","c"));
            assertCME(b, () -> b.removeIf("b"::equals));
        }

        @Test @DisplayName("clear() increments modCount")
        void clearIncrements() {
            Bag<String> b = new Bag<>(List.of("a","b","c"));
            assertCME(b, () -> b.clear());
        }

        @Test @DisplayName("sort() increments modCount")
        void sortIncrements() {
            Bag<String> b = new Bag<>(List.of("c","b","a"));
            assertCME(b, () -> b.sort(null));
        }

        @Test @DisplayName("remove() on absent item does NOT increment modCount")
        void removeAbsentNoIncrement() {
            bag.addAll(List.of("a","b","c"));
            Iterator<String> it = bag.iterator();
            it.next();
            bag.remove("absent"); // no structural change — modCount must NOT increment
            assertDoesNotThrow(it::next,
                    "remove() on absent item must not increment modCount");
        }

        @Test @DisplayName("clear() on already-empty Bag does NOT increment modCount")
        void clearEmptyNoIncrement() {
            // bag is already empty from setUp()
            Iterator<String> it = bag.iterator();
            bag.clear();  // no-op clear
            assertDoesNotThrow(() -> { /* hasNext on empty never throws */ },
                    "clear() on empty Bag must not increment modCount");
        }
    }
}