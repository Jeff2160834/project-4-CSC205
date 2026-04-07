package com.example.iterable;

import java.util.Iterator;

/**
 * BagDriver – Smoke-test driver for the Bag<E> class.
 *
 * Run this class directly (no test framework required).
 * Each section prints a clear PASS / FAIL result so you can
 * confirm every Container method and iterator behavior at a glance.
 *
 * Sections:
 *   1. Empty Bag state
 *   2. add()
 *   3. contains()
 *   4. remove()
 *   5. size() and isEmpty() after mutations
 *   6. Iterator – basic traversal
 *   7. Iterator – remove()
 *   8. Iterator – exception guards
 *   9. Null handling
 *  10. Duplicate items
 *  11. Generic type flexibility
 */
public class BagDriver {

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Bag<E>  —  Smoke Test Driver");
        System.out.println("=================================================\n");

        int passed = 0;
        int failed = 0;

        // Tally is tracked through a simple two-element int[] {passed, failed}
        // so helper methods can update it without extra scaffolding.
        int[] tally = {0, 0};

        smokeEmptyBag(tally);
        smokeAdd(tally);
        smokeContains(tally);
        smokeRemove(tally);
        smokeSizeAndIsEmpty(tally);
        smokeIteratorTraversal(tally);
        smokeIteratorRemove(tally);
        smokeIteratorExceptions(tally);
        smokeNullHandling(tally);
        smokeDuplicates(tally);
        smokeGenericTypes(tally);

        passed = tally[0];
        failed = tally[1];

        System.out.println("\n=================================================");
        System.out.printf("  Results:  %d passed  |  %d failed%n", passed, failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1); // non-zero exit signals CI failure
        }
    }

    // ==================================================================
    // 1. Empty Bag state
    // ==================================================================

    static void smokeEmptyBag(int[] tally) {
        printHeader("1. Empty Bag State");

        Bag<String> bag = new Bag<>();

        check("isEmpty() returns true on new Bag",       bag.isEmpty(),               tally);
        check("size() returns 0 on new Bag",             bag.size() == 0,             tally);
        check("contains() returns false on empty Bag",   !bag.contains("anything"),   tally);
        check("remove() returns false on empty Bag",     !bag.remove("ghost"),        tally);
        check("iterator hasNext() is false on empty Bag",!bag.iterator().hasNext(),   tally);

        // iterator.next() on empty must throw NoSuchElementException
        boolean threw = false;
        try {
            bag.iterator().next();
        } catch (java.util.NoSuchElementException e) {
            threw = true;
        }
        check("iterator.next() throws on empty Bag", threw, tally);
    }

    // ==================================================================
    // 2. add()
    // ==================================================================

    static void smokeAdd(int[] tally) {
        printHeader("2. add()");

        Bag<String> bag = new Bag<>();

        bag.add("alpha");
        check("size is 1 after one add",        bag.size() == 1,    tally);
        check("isEmpty() is false after add",   !bag.isEmpty(),     tally);

        bag.add("beta");
        bag.add("gamma");
        check("size is 3 after three adds",     bag.size() == 3,    tally);

        // Verify insertion order via iterator
        java.util.List<String> order = toList(bag);
        check("insertion order preserved: first",  "alpha".equals(order.get(0)), tally);
        check("insertion order preserved: second", "beta".equals(order.get(1)),  tally);
        check("insertion order preserved: third",  "gamma".equals(order.get(2)), tally);

        // add null
        bag.add(null);
        check("add(null) increases size",       bag.size() == 4,    tally);
    }

    // ==================================================================
    // 3. contains()
    // ==================================================================

    static void smokeContains(int[] tally) {
        printHeader("3. contains()");

        Bag<String> bag = new Bag<>();
        bag.add("hello");
        bag.add("world");

        check("contains() true for added item",         bag.contains("hello"),    tally);
        check("contains() true for second added item",  bag.contains("world"),    tally);
        check("contains() false for absent item",       !bag.contains("missing"), tally);

        bag.add(null);
        check("contains(null) true after null added",   bag.contains(null),       tally);
        check("contains() false for non-null after null added", !bag.contains("x"), tally);
    }

    // ==================================================================
    // 4. remove()
    // ==================================================================

    static void smokeRemove(int[] tally) {
        printHeader("4. remove()");

        Bag<String> bag = new Bag<>();
        bag.add("A");
        bag.add("B");
        bag.add("C");

        check("remove() returns true for existing item",   bag.remove("B"),      tally);
        check("size decreases after remove",               bag.size() == 2,      tally);
        check("removed item no longer present",            !bag.contains("B"),   tally);

        check("remove() returns false for absent item",    !bag.remove("Z"),     tally);
        check("size unchanged after failed remove",        bag.size() == 2,      tally);

        bag.remove("A");
        bag.remove("C");
        check("Bag is empty after removing all items",     bag.isEmpty(),        tally);

        // remove(null)
        Bag<String> nullBag = new Bag<>();
        nullBag.add(null);
        check("remove(null) returns true",                 nullBag.remove(null), tally);
        check("size is 0 after removing null",             nullBag.size() == 0,  tally);
    }

    // ==================================================================
    // 5. size() and isEmpty() after mutations
    // ==================================================================

    static void smokeSizeAndIsEmpty(int[] tally) {
        printHeader("5. size() and isEmpty() After Mutations");

        Bag<Integer> bag = new Bag<>();

        check("isEmpty true initially",        bag.isEmpty(),          tally);
        check("size is 0 initially",           bag.size() == 0,        tally);

        bag.add(1);
        check("isEmpty false after add",       !bag.isEmpty(),         tally);
        check("size is 1 after one add",       bag.size() == 1,        tally);

        bag.add(2);
        bag.add(3);
        check("size is 3 after three adds",    bag.size() == 3,        tally);

        bag.remove(2);
        check("size is 2 after one remove",    bag.size() == 2,        tally);

        bag.remove(1);
        bag.remove(3);
        check("isEmpty true after all removed",bag.isEmpty(),          tally);
        check("size is 0 after all removed",   bag.size() == 0,        tally);
    }

    // ==================================================================
    // 6. Iterator – basic traversal
    // ==================================================================

    static void smokeIteratorTraversal(int[] tally) {
        printHeader("6. Iterator – Basic Traversal");

        Bag<String> bag = new Bag<>();
        bag.add("one");
        bag.add("two");
        bag.add("three");

        // Count iterations
        int count = 0;
        for (String ignored : bag) count++;
        check("for-each visits 3 elements",    count == 3, tally);

        // Collect in order
        java.util.List<String> collected = toList(bag);
        check("iterator order: one",   "one".equals(collected.get(0)),   tally);
        check("iterator order: two",   "two".equals(collected.get(1)),   tally);
        check("iterator order: three", "three".equals(collected.get(2)), tally);

        // hasNext idempotency
        Iterator<String> it = bag.iterator();
        boolean h1 = it.hasNext();
        boolean h2 = it.hasNext();
        check("hasNext() is idempotent", h1 && h2, tally);

        // Two independent iterators
        Iterator<String> itA = bag.iterator();
        Iterator<String> itB = bag.iterator();
        itA.next(); // advance itA
        check("independent iterators: itB still starts at first element",
                "one".equals(itB.next()), tally);
    }

    // ==================================================================
    // 7. Iterator – remove()
    // ==================================================================

    static void smokeIteratorRemove(int[] tally) {
        printHeader("7. Iterator – remove()");

        Bag<String> bag = new Bag<>();
        bag.add("keep");
        bag.add("delete");
        bag.add("keep-too");

        Iterator<String> it = bag.iterator();
        while (it.hasNext()) {
            if ("delete".equals(it.next())) {
                it.remove();
            }
        }

        check("size is 2 after iterator remove",      bag.size() == 2,             tally);
        check("deleted item gone after iterator remove", !bag.contains("delete"),  tally);
        check("first kept item still present",         bag.contains("keep"),        tally);
        check("second kept item still present",        bag.contains("keep-too"),    tally);

        // Remove all via iterator
        Bag<String> bag2 = new Bag<>();
        bag2.add("x");
        bag2.add("y");
        Iterator<String> it2 = bag2.iterator();
        while (it2.hasNext()) {
            it2.next();
            it2.remove();
        }
        check("Bag is empty after removing all via iterator", bag2.isEmpty(), tally);
    }

    // ==================================================================
    // 8. Iterator – exception guards
    // ==================================================================

    static void smokeIteratorExceptions(int[] tally) {
        printHeader("8. Iterator – Exception Guards");

        Bag<String> bag = new Bag<>();
        bag.add("item");

        // next() past end → NoSuchElementException
        Iterator<String> it = bag.iterator();
        it.next();
        boolean nseThrown = false;
        try {
            it.next();
        } catch (java.util.NoSuchElementException e) {
            nseThrown = true;
        }
        check("next() past end throws NoSuchElementException", nseThrown, tally);

        // remove() before next() → IllegalStateException
        Iterator<String> it2 = bag.iterator();
        boolean iseThrown = false;
        try {
            it2.remove();
        } catch (IllegalStateException e) {
            iseThrown = true;
        }
        check("remove() before next() throws IllegalStateException", iseThrown, tally);

        // remove() twice in a row → IllegalStateException
        Iterator<String> it3 = bag.iterator();
        it3.next();
        it3.remove();
        boolean ise2Thrown = false;
        try {
            it3.remove();
        } catch (IllegalStateException e) {
            ise2Thrown = true;
        }
        check("remove() twice in a row throws IllegalStateException", ise2Thrown, tally);
    }

    // ==================================================================
    // 9. Null handling
    // ==================================================================

    static void smokeNullHandling(int[] tally) {
        printHeader("9. Null Handling");

        Bag<String> bag = new Bag<>();
        bag.add(null);
        bag.add(null);
        bag.add("real");

        check("size is 3 with two nulls",             bag.size() == 3,      tally);
        check("contains(null) is true",               bag.contains(null),   tally);
        check("contains real item alongside nulls",   bag.contains("real"), tally);

        bag.remove(null); // removes first null only
        check("size is 2 after removing one null",    bag.size() == 2,      tally);
        check("contains(null) still true (one left)", bag.contains(null),   tally);

        bag.remove(null);
        check("contains(null) false after both nulls removed", !bag.contains(null), tally);

        // Iterator visits null without throwing
        Bag<String> bag2 = new Bag<>();
        bag2.add("before");
        bag2.add(null);
        bag2.add("after");
        java.util.List<String> list = toList(bag2);
        check("iterator visits null element without throwing", list.size() == 3,   tally);
        check("null is at expected position in iteration",     list.get(1) == null, tally);
    }

    // ==================================================================
    // 10. Duplicate items
    // ==================================================================

    static void smokeDuplicates(int[] tally) {
        printHeader("10. Duplicate Items");

        Bag<String> bag = new Bag<>();
        bag.add("dup");
        bag.add("dup");
        bag.add("dup");

        check("size is 3 with three duplicates",      bag.size() == 3,    tally);
        check("contains() true with duplicates",      bag.contains("dup"), tally);

        bag.remove("dup"); // removes first occurrence only
        check("size is 2 after removing one duplicate", bag.size() == 2,  tally);
        check("contains() still true after partial remove", bag.contains("dup"), tally);

        // Iterator counts all duplicates
        int count = 0;
        for (String s : bag) if ("dup".equals(s)) count++;
        check("iterator counts remaining duplicates (2)", count == 2, tally);

        bag.remove("dup");
        bag.remove("dup");
        check("Bag empty after all duplicates removed", bag.isEmpty(), tally);
    }

    // ==================================================================
    // 11. Generic type flexibility
    // ==================================================================

    static void smokeGenericTypes(int[] tally) {
        printHeader("11. Generic Type Flexibility");

        // Integer Bag
        Bag<Integer> intBag = new Bag<>();
        for (int i = 1; i <= 5; i++) intBag.add(i);
        int sum = 0;
        for (int n : intBag) sum += n;
        check("Integer Bag sums 1-5 correctly (sum=15)", sum == 15, tally);

        // Double Bag
        Bag<Double> dblBag = new Bag<>();
        dblBag.add(1.5);
        dblBag.add(2.5);
        check("Double Bag contains 1.5", dblBag.contains(1.5), tally);

        // Custom object Bag
        Bag<int[]> arrBag = new Bag<>();
        int[] arr = {10, 20, 30};
        arrBag.add(arr);
        check("Bag<int[]> contains the added array", arrBag.contains(arr), tally);

        // Bag<Object> with mixed runtime types
        Bag<Object> mixed = new Bag<>();
        mixed.add("string");
        mixed.add(99);
        mixed.add(3.14);
        mixed.add(null);
        check("Bag<Object> size is 4 with mixed types", mixed.size() == 4,      tally);
        check("Bag<Object> contains String",             mixed.contains("string"), tally);
        check("Bag<Object> contains Integer",            mixed.contains(99),       tally);
        check("Bag<Object> contains null",               mixed.contains(null),     tally);
    }

    // ==================================================================
    // Utility helpers
    // ==================================================================

    /** Prints a section header. */
    private static void printHeader(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    /**
     * Evaluates a condition and prints PASS / FAIL.
     * Updates tally[0] (passed) or tally[1] (failed).
     */
    private static void check(String description, boolean condition, int[] tally) {
        if (condition) {
            System.out.printf("  [ PASS ]  %s%n", description);
            tally[0]++;
        } else {
            System.out.printf("  [ FAIL ]  %s%n", description);
            tally[1]++;
        }
    }

    /** Collects all elements of an Iterable into an ArrayList. */
    private static <T> java.util.List<T> toList(Iterable<T> iterable) {
        java.util.List<T> list = new java.util.ArrayList<>();
        for (T item : iterable) list.add(item);
        return list;
    }
}