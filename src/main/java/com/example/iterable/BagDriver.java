package com.example.iterable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * BagDriver – Smoke-test driver for the optimized Bag<E> class.
 *
 * Sections:
 *   1.  Constructor variants (default, capacity, collection)
 *   2.  add() and addAll()
 *   3.  contains() and frequency()
 *   4.  remove(), removeAll(), removeIf()
 *   5.  size() and isEmpty()
 *   6.  Capacity management – ensureCapacity() and trimToSize()
 *   7.  clear()
 *   8.  Iterator – live traversal
 *   9.  Iterator – snapshot (snapshotIterator)
 *  10.  Iterator – exception guards
 *  11.  sort()
 *  12.  toUnmodifiableList(), toArray(), copy()
 *  13.  Null handling
 *  14.  Duplicate items
 *  15.  Generic type flexibility
 */
public class BagDriver {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     Bag<E> Optimized  —  Smoke Test Driver        ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");

        int[] tally = {0, 0};

        smokeConstructors(tally);
        smokeAddAndAddAll(tally);
        smokeContainsAndFrequency(tally);
        smokeRemoveVariants(tally);
        smokeSizeAndIsEmpty(tally);
        smokeCapacityManagement(tally);
        smokeClear(tally);
        smokeLiveIterator(tally);
        smokeSnapshotIterator(tally);
        smokeIteratorExceptions(tally);
        smokeSort(tally);
        smokeBulkUtilities(tally);
        smokeNullHandling(tally);
        smokeDuplicates(tally);
        smokeGenericTypes(tally);

        System.out.println("\n╔═══════════════════════════════════════════════════╗");
        System.out.printf( "║  Results:  %3d passed  |  %3d failed             ║%n",
                tally[0], tally[1]);
        System.out.println("╚═══════════════════════════════════════════════════╝");

        if (tally[1] > 0) System.exit(1);
    }

    // ==================================================================
    // 1. Constructor variants
    // ==================================================================

    static void smokeConstructors(int[] tally) {
        printHeader("1. Constructor Variants");

        // Default constructor
        Bag<String> b1 = new Bag<>();
        printBag("new Bag<>()", b1);
        check("default constructor: empty",      b1.isEmpty(),   tally);
        check("default constructor: size 0",     b1.size() == 0, tally);

        // Capacity constructor
        Bag<String> b2 = new Bag<>(50);
        printBag("new Bag<>(50)", b2);
        check("capacity constructor: empty",     b2.isEmpty(),   tally);

        // Collection constructor
        Bag<String> b3 = new Bag<>(List.of("x", "y", "z"));
        printBag("new Bag<>(List.of(\"x\",\"y\",\"z\"))", b3);
        check("collection constructor: size 3",  b3.size() == 3, tally);
        check("collection constructor: has x",   b3.contains("x"), tally);
        check("collection constructor: has z",   b3.contains("z"), tally);

        // Negative capacity
        boolean threw = false;
        try { new Bag<>(-1); } catch (IllegalArgumentException e) { threw = true; }
        check("negative capacity throws IllegalArgumentException", threw, tally);
    }

    // ==================================================================
    // 2. add() and addAll()
    // ==================================================================

    static void smokeAddAndAddAll(int[] tally) {
        printHeader("2. add() and addAll()");

        Bag<String> bag = new Bag<>();
        printBag("Before any adds", bag);

        bag.add("alpha");
        printBag("After add(\"alpha\")", bag);
        check("size 1 after one add",    bag.size() == 1, tally);

        bag.add("beta");
        bag.add("gamma");
        printBag("After add(\"beta\"), add(\"gamma\")", bag);
        check("size 3 after three adds", bag.size() == 3, tally);

        // addAll
        bag.addAll(List.of("delta", "epsilon", "zeta"));
        printBag("After addAll([delta, epsilon, zeta])", bag);
        check("size 6 after addAll of 3",    bag.size() == 6,         tally);
        check("addAll: contains delta",      bag.contains("delta"),   tally);
        check("addAll: contains epsilon",    bag.contains("epsilon"), tally);
        check("addAll: contains zeta",       bag.contains("zeta"),    tally);

        // addAll null guard
        boolean threw = false;
        try { bag.addAll(null); } catch (NullPointerException e) { threw = true; }
        check("addAll(null) throws NullPointerException", threw, tally);
    }

    // ==================================================================
    // 3. contains() and frequency()
    // ==================================================================

    static void smokeContainsAndFrequency(int[] tally) {
        printHeader("3. contains() and frequency()");

        Bag<String> bag = new Bag<>();
        bag.add("a");
        bag.add("b");
        bag.add("a");
        bag.add("a");
        printBag("Bag [a, b, a, a]", bag);

        check("contains(\"a\") true",          bag.contains("a"),       tally);
        check("contains(\"b\") true",          bag.contains("b"),       tally);
        check("contains(\"z\") false",         !bag.contains("z"),      tally);
        check("frequency(\"a\") == 3",         bag.frequency("a") == 3, tally);
        check("frequency(\"b\") == 1",         bag.frequency("b") == 1, tally);
        check("frequency(\"z\") == 0",         bag.frequency("z") == 0, tally);

        // Empty-bag short-circuit
        Bag<String> empty = new Bag<>();
        check("contains() false on empty (short-circuit)", !empty.contains("x"),  tally);
        check("frequency() 0 on empty (short-circuit)",    empty.frequency("x") == 0, tally);
    }

    // ==================================================================
    // 4. remove(), removeAll(), removeIf()
    // ==================================================================

    static void smokeRemoveVariants(int[] tally) {
        printHeader("4. remove(), removeAll(), removeIf()");

        // --- remove(E) ---
        Bag<String> bag = new Bag<>();
        bag.addAll(List.of("A", "B", "C"));
        printBag("Initial [A, B, C]", bag);

        check("remove(\"B\") returns true",  bag.remove("B"),    tally);
        printBag("After remove(\"B\")", bag);
        check("size 2 after remove",         bag.size() == 2,    tally);
        check("B gone after remove",         !bag.contains("B"), tally);
        check("remove(\"Z\") returns false", !bag.remove("Z"),   tally);

        // --- removeAll(E, int) ---
        Bag<String> dup = new Bag<>(List.of("x", "x", "x", "x", "y"));
        printBag("Dup bag [x,x,x,x,y] before removeAll(x, 2)", dup);
        int removed = dup.removeAll("x", 2);
        printBag("After removeAll(\"x\", 2)", dup);
        check("removeAll returned 2",          removed == 2,         tally);
        check("size is 3 after removeAll",     dup.size() == 3,      tally);
        check("x still present (2 remain)",    dup.contains("x"),    tally);
        check("y unaffected by removeAll",     dup.contains("y"),    tally);

        // remove ALL occurrences
        int removedAll = dup.removeAll("x", Integer.MAX_VALUE);
        printBag("After removeAll(\"x\", MAX_VALUE)", dup);
        check("removeAll(MAX_VALUE) removed 2 remaining", removedAll == 2,     tally);
        check("x completely gone",                        !dup.contains("x"),  tally);

        // negative maxCount guard
        boolean threw = false;
        try { dup.removeAll("y", -1); } catch (IllegalArgumentException e) { threw = true; }
        check("removeAll with negative maxCount throws", threw, tally);

        // --- removeIf(Predicate) ---
        Bag<Integer> nums = new Bag<>(List.of(1, 2, 3, 4, 5, 6));
        printBag("Nums [1..6] before removeIf(even)", nums);
        boolean changed = nums.removeIf(n -> n % 2 == 0);
        printBag("After removeIf(even)", nums);
        check("removeIf returned true",         changed,           tally);
        check("size is 3 after removeIf",       nums.size() == 3,  tally);
        check("no even numbers remain",         !nums.contains(2) && !nums.contains(4) && !nums.contains(6), tally);
        check("odd numbers still present",      nums.contains(1) && nums.contains(3) && nums.contains(5),   tally);

        // removeIf null guard
        boolean threwNpe = false;
        try { nums.removeIf(null); } catch (NullPointerException e) { threwNpe = true; }
        check("removeIf(null) throws NullPointerException", threwNpe, tally);
    }

    // ==================================================================
    // 5. size() and isEmpty()
    // ==================================================================

    static void smokeSizeAndIsEmpty(int[] tally) {
        printHeader("5. size() and isEmpty()");

        Bag<Integer> bag = new Bag<>();
        printBag("Initial", bag);
        check("isEmpty true initially",  bag.isEmpty(),    tally);
        check("size 0 initially",        bag.size() == 0,  tally);

        bag.add(10);
        printBag("After add(10)", bag);
        check("isEmpty false after add", !bag.isEmpty(),   tally);
        check("size 1 after add",        bag.size() == 1,  tally);

        bag.add(20);
        bag.add(30);
        printBag("After add(20), add(30)", bag);
        check("size 3 after three adds", bag.size() == 3,  tally);

        bag.remove(20);
        printBag("After remove(20)", bag);
        check("size 2 after remove",     bag.size() == 2,  tally);
    }

    // ==================================================================
    // 6. Capacity management
    // ==================================================================

    static void smokeCapacityManagement(int[] tally) {
        printHeader("6. Capacity Management – ensureCapacity() and trimToSize()");

        Bag<Integer> bag = new Bag<>();
        bag.ensureCapacity(1000);
        System.out.println("  ensureCapacity(1000) called — no resize during bulk insert");
        for (int i = 0; i < 1000; i++) bag.add(i);
        printBag("After 1000 adds (ensureCapacity pre-allocated)", bag);
        check("size is 1000 after 1000 adds", bag.size() == 1000, tally);

        // Trim back down
        bag.removeIf(n -> n >= 5);
        printBag("After removeIf(n >= 5) — only 0-4 remain", bag);
        bag.trimToSize();
        System.out.println("  trimToSize() called — excess backing array capacity released");
        check("size is 5 after trimToSize",   bag.size() == 5,   tally);
        check("elements intact after trim",   bag.contains(0) && bag.contains(4), tally);
    }

    // ==================================================================
    // 7. clear()
    // ==================================================================

    static void smokeClear(int[] tally) {
        printHeader("7. clear()");

        Bag<String> bag = new Bag<>(List.of("a", "b", "c", "d"));
        printBag("Before clear()", bag);
        bag.clear();
        printBag("After clear()", bag);
        check("isEmpty() true after clear", bag.isEmpty(),   tally);
        check("size is 0 after clear",      bag.size() == 0, tally);

        // Bag is reusable after clear
        bag.add("fresh");
        printBag("After add(\"fresh\") post-clear — Bag reusable", bag);
        check("Bag reusable after clear",   bag.contains("fresh"), tally);
    }

    // ==================================================================
    // 8. Iterator – live traversal
    // ==================================================================

    static void smokeLiveIterator(int[] tally) {
        printHeader("8. Iterator – Live Traversal");

        Bag<String> bag = new Bag<>(List.of("one", "two", "three"));
        printBag("Bag for live iteration", bag);

        System.out.println("  Iterating with for-each:");
        int count = 0;
        for (String s : bag) {
            System.out.printf("    cursor[%d] → \"%s\"%n", count++, s);
        }
        check("for-each visits all 3 elements", count == 3, tally);

        // Live iterator remove
        Iterator<String> it = bag.iterator();
        while (it.hasNext()) {
            if ("two".equals(it.next())) it.remove();
        }
        printBag("After live iterator-remove of \"two\"", bag);
        check("\"two\" removed via live iterator",  !bag.contains("two"), tally);
        check("\"one\" and \"three\" intact",        bag.contains("one") && bag.contains("three"), tally);
    }

    // ==================================================================
    // 9. Iterator – snapshot
    // ==================================================================

    static void smokeSnapshotIterator(int[] tally) {
        printHeader("9. Iterator – Snapshot");

        Bag<String> bag = new Bag<>(List.of("p", "q", "r"));
        printBag("Bag before snapshot", bag);

        Iterator<String> snap = bag.snapshotIterator();

        // Mutate the live Bag WHILE the snapshot iterator exists
        bag.add("s");
        bag.remove("p");
        printBag("Live Bag mutated (added s, removed p) — snapshot unaffected", bag);

        System.out.println("  Snapshot iterator sees:");
        java.util.List<String> snapElements = new java.util.ArrayList<>();
        while (snap.hasNext()) {
            String val = snap.next();
            snapElements.add(val);
            System.out.printf("    → \"%s\"%n", val);
        }
        check("snapshot sees original 3 elements",  snapElements.size() == 3,     tally);
        check("snapshot sees \"p\" (removed live)",  snapElements.contains("p"),   tally);
        check("snapshot does NOT see \"s\" (added live)", !snapElements.contains("s"), tally);

        // Snapshot remove() must throw
        Iterator<String> snap2 = bag.snapshotIterator();
        snap2.next();
        boolean threw = false;
        try { snap2.remove(); } catch (UnsupportedOperationException e) { threw = true; }
        check("snapshot iterator remove() throws UnsupportedOperationException", threw, tally);
    }

    // ==================================================================
    // 10. Iterator – exception guards
    // ==================================================================

    static void smokeIteratorExceptions(int[] tally) {
        printHeader("10. Iterator – Exception Guards");

        Bag<String> bag = new Bag<>(List.of("only"));
        printBag("Single-element Bag", bag);

        // next() past end
        Iterator<String> it = bag.iterator();
        it.next();
        boolean nse = false;
        try { it.next(); } catch (java.util.NoSuchElementException e) { nse = true; }
        check("next() past end throws NoSuchElementException", nse, tally);

        // remove() before next()
        Iterator<String> it2 = bag.iterator();
        boolean ise1 = false;
        try { it2.remove(); } catch (IllegalStateException e) { ise1 = true; }
        check("remove() before next() throws IllegalStateException", ise1, tally);

        // remove() twice in a row
        Iterator<String> it3 = bag.iterator();
        it3.next();
        it3.remove();
        boolean ise2 = false;
        try { it3.remove(); } catch (IllegalStateException e) { ise2 = true; }
        check("remove() twice in a row throws IllegalStateException", ise2, tally);
    }

    // ==================================================================
    // 11. sort()
    // ==================================================================

    static void smokeSort(int[] tally) {
        printHeader("11. sort()");

        Bag<Integer> bag = new Bag<>(List.of(5, 3, 1, 4, 2));
        printBag("Before sort (natural order)", bag);
        bag.sort(null);   // null → natural ordering
        printBag("After sort(null) — ascending", bag);

        java.util.List<Integer> sorted = new java.util.ArrayList<>();
        for (int n : bag) sorted.add(n);
        check("sort(null) ascending: 1,2,3,4,5",
                sorted.equals(java.util.List.of(1, 2, 3, 4, 5)), tally);

        // Reverse sort
        bag.sort(java.util.Comparator.reverseOrder());
        printBag("After sort(reverseOrder)", bag);
        java.util.List<Integer> rev = new java.util.ArrayList<>();
        for (int n : bag) rev.add(n);
        check("sort(reverseOrder) descending: 5,4,3,2,1",
                rev.equals(java.util.List.of(5, 4, 3, 2, 1)), tally);

        // String sort
        Bag<String> words = new Bag<>(List.of("banana", "apple", "cherry"));
        printBag("String Bag before sort", words);
        words.sort(java.util.Comparator.naturalOrder());
        printBag("String Bag after sort(naturalOrder)", words);
        java.util.List<String> wordsSorted = new java.util.ArrayList<>();
        for (String w : words) wordsSorted.add(w);
        check("String sort: apple, banana, cherry",
                wordsSorted.equals(java.util.List.of("apple", "banana", "cherry")), tally);
    }

    // ==================================================================
    // 12. toUnmodifiableList(), toArray(), copy()
    // ==================================================================

    static void smokeBulkUtilities(int[] tally) {
        printHeader("12. toUnmodifiableList(), toArray(), copy()");

        Bag<String> bag = new Bag<>(List.of("alpha", "beta", "gamma"));
        printBag("Source Bag", bag);

        // toUnmodifiableList
        java.util.List<String> view = bag.toUnmodifiableList();
        check("toUnmodifiableList size is 3",   view.size() == 3,         tally);
        check("toUnmodifiableList contains alpha", view.contains("alpha"), tally);
        boolean threw = false;
        try { view.add("illegal"); } catch (UnsupportedOperationException e) { threw = true; }
        check("toUnmodifiableList is read-only", threw, tally);

        // toArray
        Object[] arr = bag.toArray();
        check("toArray length is 3",           arr.length == 3,             tally);
        check("toArray[0] is alpha",           "alpha".equals(arr[0]),      tally);

        // copy
        Bag<String> copied = bag.copy();
        printBag("copy() result", copied);
        check("copy has same size",            copied.size() == bag.size(), tally);
        check("copy contains alpha",           copied.contains("alpha"),    tally);
        // Mutations to copy don't affect original
        copied.add("delta");
        check("copy independence: original unchanged", bag.size() == 3,    tally);
        check("copy independence: copy grew",          copied.size() == 4, tally);
        printBag("After copy.add(\"delta\") — original untouched", bag);
        printBag("Copied Bag with extra element", copied);
    }

    // ==================================================================
    // 13. Null handling
    // ==================================================================

    static void smokeNullHandling(int[] tally) {
        printHeader("13. Null Handling");

        Bag<String> bag = new Bag<>();
        bag.add(null);
        bag.add(null);
        bag.add("real");
        printBag("After add(null), add(null), add(\"real\")", bag);

        check("contains(null) true",           bag.contains(null),         tally);
        check("frequency(null) == 2",          bag.frequency(null) == 2,   tally);

        int rm = bag.removeAll(null, 1);
        printBag("After removeAll(null, 1)", bag);
        check("removeAll(null,1) removed 1",   rm == 1,                    tally);
        check("one null still present",        bag.contains(null),         tally);

        // removeIf with null match
        bag.removeIf(e -> e == null);
        printBag("After removeIf(null check)", bag);
        check("no nulls remain after removeIf",!bag.contains(null),        tally);
        check("real item intact",               bag.contains("real"),      tally);
    }

    // ==================================================================
    // 14. Duplicate items
    // ==================================================================

    static void smokeDuplicates(int[] tally) {
        printHeader("14. Duplicate Items");

        Bag<String> bag = new Bag<>(List.of("dup", "dup", "dup", "dup"));
        printBag("Four \"dup\" entries", bag);
        check("frequency(\"dup\") == 4",       bag.frequency("dup") == 4, tally);

        int rm = bag.removeAll("dup", 2);
        printBag("After removeAll(\"dup\", 2)", bag);
        check("removeAll removed 2",           rm == 2,                   tally);
        check("frequency now 2",               bag.frequency("dup") == 2, tally);

        bag.removeIf("dup"::equals);
        printBag("After removeIf — all dups gone", bag);
        check("no dups remain",                !bag.contains("dup"),      tally);
        check("isEmpty after removing all dups", bag.isEmpty(),           tally);
    }

    // ==================================================================
    // 15. Generic type flexibility
    // ==================================================================

    static void smokeGenericTypes(int[] tally) {
        printHeader("15. Generic Type Flexibility");

        // Bag<Integer>
        Bag<Integer> intBag = new Bag<>(List.of(1, 2, 3, 4, 5));
        printBag("Bag<Integer> [1..5]", intBag);
        intBag.sort(null);
        int sum = 0;
        for (int n : intBag) sum += n;
        check("Bag<Integer> sorted sum 1-5 = 15", sum == 15, tally);

        // Bag<Double>
        Bag<Double> dblBag = new Bag<>(List.of(3.14, 1.0, 2.72));
        dblBag.sort(null);
        printBag("Bag<Double> sorted", dblBag);
        Object[] dArr = dblBag.toArray();
        check("Bag<Double> sorted[0] is 1.0", Double.valueOf(1.0).equals(dArr[0]), tally);

        // Bag<Object> mixed
        Bag<Object> mixed = new Bag<>();
        mixed.add("text");
        mixed.add(42);
        mixed.add(null);
        printBag("Bag<Object> mixed types", mixed);
        check("Bag<Object> size 3",           mixed.size() == 3,        tally);
        check("Bag<Object> frequency(null)=1",mixed.frequency(null) == 1, tally);

        // copy() on generic bag
        Bag<Object> mixedCopy = mixed.copy();
        check("copy of Bag<Object> equal",    mixed.equals(mixedCopy), tally);
    }

    // ==================================================================
    // Visual helper – printBag()
    // ==================================================================

    private static <T> void printBag(String label, Bag<T> bag) {
        final int CELL = 10;
        java.util.List<T> elements = toList(bag);
        int size = elements.size();

        System.out.printf("%n  ArrayList  size=%-4d [%s]%n", size, label);

        if (size == 0) {
            int w = CELL + 4;
            System.out.println("  ┌" + "─".repeat(w) + "┐");
            System.out.printf( "  │ %-" + w + "s│%n", "    (empty)");
            System.out.println("  └" + "─".repeat(w) + "┘");
            return;
        }

        String[] idx = new String[size];
        String[] val = new String[size];
        for (int i = 0; i < size; i++) {
            idx[i] = fit("[" + i + "]",              CELL);
            val[i] = fit(renderValue(elements.get(i)), CELL);
        }

        StringBuilder top = new StringBuilder("  ┌");
        StringBuilder ir  = new StringBuilder("  │");
        StringBuilder vr  = new StringBuilder("  │");
        StringBuilder bot = new StringBuilder("  └");

        for (int i = 0; i < size; i++) {
            top.append("─".repeat(CELL + 2)).append(i < size - 1 ? "┬" : "┐");
            ir .append(" ").append(idx[i]).append(" │");
            vr .append(" ").append(val[i]).append(" │");
            bot.append("─".repeat(CELL + 2)).append(i < size - 1 ? "┴" : "┘");
        }
        System.out.println(top);
        System.out.println(ir);
        System.out.println(vr);
        System.out.println(bot);
    }

    private static String renderValue(Object v) {
        if (v == null) return "<null>";
        if (v instanceof int[]) {
            int[] a = (int[]) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) { sb.append(a[i]); if (i < a.length-1) sb.append(","); }
            return sb.append("]").toString();
        }
        return v.toString();
    }

    private static String fit(String s, int w) {
        if (s.length() > w) return s.substring(0, w - 1) + "…";
        return String.format("%-" + w + "s", s);
    }

    private static void printHeader(String title) {
        System.out.println("\n┌───────────────────────────────────────────────────┐");
        System.out.printf( "│  %-49s│%n", title);
        System.out.println("└───────────────────────────────────────────────────┘");
    }

    private static void check(String desc, boolean condition, int[] tally) {
        System.out.printf("  [%s]  %s%n", condition ? " PASS " : " FAIL ", desc);
        tally[condition ? 0 : 1]++;
    }

    private static <T> java.util.List<T> toList(Iterable<T> it) {
        java.util.List<T> list = new java.util.ArrayList<>();
        for (T item : it) list.add(item);
        return list;
    }
}