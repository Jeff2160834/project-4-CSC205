package com.example.iterable;

import java.util.Iterator;

/**
 * BagDriver – Smoke-test driver for the Bag<E> class.
 *
 * Every meaningful mutation (add / remove / iterator-remove) is followed
 * by a printBag() call that renders the current ArrayList state as an
 * ASCII diagram so you can watch the backing array change in real time.
 *
 * Visual format example (non-empty):
 *
 *   ArrayList  size=3   [After add("gamma")]
 *   ┌──────────┬──────────┬──────────┐
 *   │ [0]      │ [1]      │ [2]      │
 *   │ alpha    │ beta     │ gamma    │
 *   └──────────┴──────────┴──────────┘
 *
 * Visual format (empty):
 *
 *   ArrayList  size=0   [Initial state]
 *   ┌────────────────────┐
 *   │      (empty)       │
 *   └────────────────────┘
 *
 * Sections:
 *   1.  Empty Bag state
 *   2.  add()
 *   3.  contains()
 *   4.  remove()
 *   5.  size() and isEmpty() after mutations
 *   6.  Iterator – basic traversal
 *   7.  Iterator – remove()
 *   8.  Iterator – exception guards
 *   9.  Null handling
 *  10.  Duplicate items
 *  11.  Generic type flexibility
 */
public class BagDriver {

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║         Bag<E>  —  Smoke Test Driver          ║");
        System.out.println("╚═══════════════════════════════════════════════╝");

        int[] tally = {0, 0};   // { passed, failed }

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

        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.printf( "║  Results:  %3d passed  |  %3d failed          ║%n",
                tally[0], tally[1]);
        System.out.println("╚═══════════════════════════════════════════════╝");

        if (tally[1] > 0) System.exit(1);
    }

    // ==================================================================
    // 1. Empty Bag state
    // ==================================================================

    static void smokeEmptyBag(int[] tally) {
        printHeader("1. Empty Bag State");

        Bag<String> bag = new Bag<>();
        printBag("Initial state — nothing added yet", bag);

        check("isEmpty() returns true on new Bag",        bag.isEmpty(),             tally);
        check("size() returns 0 on new Bag",              bag.size() == 0,           tally);
        check("contains() returns false on empty Bag",    !bag.contains("anything"), tally);
        check("remove() returns false on empty Bag",      !bag.remove("ghost"),      tally);
        check("iterator hasNext() is false on empty Bag", !bag.iterator().hasNext(), tally);

        boolean threw = false;
        try   { bag.iterator().next(); }
        catch (java.util.NoSuchElementException e) { threw = true; }
        check("iterator.next() throws NoSuchElementException on empty Bag", threw, tally);
    }

    // ==================================================================
    // 2. add()
    // ==================================================================

    static void smokeAdd(int[] tally) {
        printHeader("2. add()");

        Bag<String> bag = new Bag<>();
        printBag("Before any adds", bag);

        bag.add("alpha");
        printBag("After add(\"alpha\")", bag);
        check("size is 1 after one add",      bag.size() == 1, tally);
        check("isEmpty() is false after add", !bag.isEmpty(),  tally);

        bag.add("beta");
        printBag("After add(\"beta\")", bag);

        bag.add("gamma");
        printBag("After add(\"gamma\")", bag);
        check("size is 3 after three adds", bag.size() == 3, tally);

        java.util.List<String> order = toList(bag);
        check("insertion order preserved – index 0 is \"alpha\"", "alpha".equals(order.get(0)), tally);
        check("insertion order preserved – index 1 is \"beta\"",  "beta".equals(order.get(1)),  tally);
        check("insertion order preserved – index 2 is \"gamma\"", "gamma".equals(order.get(2)), tally);

        bag.add(null);
        printBag("After add(null)", bag);
        check("add(null) increases size to 4", bag.size() == 4, tally);
    }

    // ==================================================================
    // 3. contains()
    // ==================================================================

    static void smokeContains(int[] tally) {
        printHeader("3. contains()");

        Bag<String> bag = new Bag<>();
        bag.add("hello");
        bag.add("world");
        printBag("Bag used for contains() checks", bag);

        check("contains(\"hello\") – present item",             bag.contains("hello"),    tally);
        check("contains(\"world\") – second present item",      bag.contains("world"),    tally);
        check("contains(\"missing\") – absent item is false",   !bag.contains("missing"), tally);

        bag.add(null);
        printBag("After add(null)", bag);
        check("contains(null) – true after null added",            bag.contains(null),   tally);
        check("contains(\"x\") – false; only null was just added", !bag.contains("x"),   tally);
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
        printBag("Initial state [A, B, C]", bag);

        check("remove(\"B\") returns true – item exists",   bag.remove("B"),    tally);
        printBag("After remove(\"B\")", bag);
        check("size is 2 after removing \"B\"",             bag.size() == 2,    tally);
        check("contains(\"B\") is false after removal",     !bag.contains("B"), tally);

        check("remove(\"Z\") returns false – item absent",  !bag.remove("Z"),   tally);
        printBag("After remove(\"Z\") – no change expected", bag);
        check("size still 2 after failed remove",            bag.size() == 2,   tally);

        bag.remove("A");
        printBag("After remove(\"A\")", bag);
        bag.remove("C");
        printBag("After remove(\"C\") — Bag now empty", bag);
        check("Bag is empty after removing all items", bag.isEmpty(), tally);

        // remove(null)
        Bag<String> nullBag = new Bag<>();
        nullBag.add(null);
        printBag("nullBag before remove(null)", nullBag);
        check("remove(null) returns true",     nullBag.remove(null), tally);
        printBag("nullBag after remove(null)", nullBag);
        check("size is 0 after removing null", nullBag.size() == 0,  tally);
    }

    // ==================================================================
    // 5. size() and isEmpty() after mutations
    // ==================================================================

    static void smokeSizeAndIsEmpty(int[] tally) {
        printHeader("5. size() and isEmpty() After Mutations");

        Bag<Integer> bag = new Bag<>();
        printBag("Initial state", bag);
        check("isEmpty() true initially",  bag.isEmpty(),   tally);
        check("size() is 0 initially",     bag.size() == 0, tally);

        bag.add(1);
        printBag("After add(1)", bag);
        check("isEmpty() false after add", !bag.isEmpty(),  tally);
        check("size() is 1 after one add", bag.size() == 1, tally);

        bag.add(2);
        printBag("After add(2)", bag);
        bag.add(3);
        printBag("After add(3)", bag);
        check("size() is 3 after three adds", bag.size() == 3, tally);

        bag.remove(2);
        printBag("After remove(2)", bag);
        check("size() is 2 after one remove", bag.size() == 2, tally);

        bag.remove(1);
        printBag("After remove(1)", bag);
        bag.remove(3);
        printBag("After remove(3) — Bag now empty", bag);
        check("isEmpty() true after all removed", bag.isEmpty(),   tally);
        check("size() is 0 after all removed",    bag.size() == 0, tally);
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
        printBag("Bag used for traversal", bag);

        System.out.println("  Iterating with for-each:");
        int count = 0;
        for (String s : bag) {
            System.out.printf("    cursor[%d] → \"%s\"%n", count, s);
            count++;
        }
        check("for-each visits exactly 3 elements", count == 3, tally);

        java.util.List<String> collected = toList(bag);
        check("element at index 0 is \"one\"",   "one".equals(collected.get(0)),   tally);
        check("element at index 1 is \"two\"",   "two".equals(collected.get(1)),   tally);
        check("element at index 2 is \"three\"", "three".equals(collected.get(2)), tally);

        // hasNext idempotency
        Iterator<String> it = bag.iterator();
        boolean h1 = it.hasNext();
        boolean h2 = it.hasNext();
        check("hasNext() is idempotent (multiple calls don't advance)", h1 && h2, tally);

        // Two independent iterators
        Iterator<String> itA = bag.iterator();
        Iterator<String> itB = bag.iterator();
        itA.next(); // advance itA to index 1
        check("itB is independent – still starts at index 0 after itA advanced",
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
        printBag("Before iterator-remove pass", bag);

        System.out.println("  Walking iterator – removing \"delete\":");
        Iterator<String> it = bag.iterator();
        while (it.hasNext()) {
            String current = it.next();
            if ("delete".equals(current)) {
                it.remove();
                System.out.println("    removed → \"delete\"");
            } else {
                System.out.printf("    kept    → \"%s\"%n", current);
            }
        }
        printBag("After iterator-remove of \"delete\"", bag);

        check("size is 2 after iterator-remove",          bag.size() == 2,          tally);
        check("\"delete\" is gone after iterator-remove", !bag.contains("delete"),  tally);
        check("\"keep\" still present",                    bag.contains("keep"),     tally);
        check("\"keep-too\" still present",                bag.contains("keep-too"), tally);

        // Remove ALL elements via iterator
        Bag<String> bag2 = new Bag<>();
        bag2.add("x");
        bag2.add("y");
        printBag("bag2 before full iterator-remove", bag2);
        System.out.println("  Removing all elements via iterator:");
        Iterator<String> it2 = bag2.iterator();
        while (it2.hasNext()) {
            String val = it2.next();
            it2.remove();
            System.out.printf("    removed → \"%s\"%n", val);
            printBag("bag2 state mid-iteration", bag2);
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
        printBag("Bag used for exception tests", bag);

        // next() past the end
        Iterator<String> it = bag.iterator();
        it.next(); // consume the only element
        boolean nseThrown = false;
        try   { it.next(); }
        catch (java.util.NoSuchElementException e) { nseThrown = true; }
        check("next() past end throws NoSuchElementException", nseThrown, tally);

        // remove() before next()
        Iterator<String> it2 = bag.iterator();
        boolean ise1Thrown = false;
        try   { it2.remove(); }
        catch (IllegalStateException e) { ise1Thrown = true; }
        check("remove() before next() throws IllegalStateException", ise1Thrown, tally);

        // remove() twice in a row
        Iterator<String> it3 = bag.iterator();
        it3.next();
        it3.remove();
        boolean ise2Thrown = false;
        try   { it3.remove(); }
        catch (IllegalStateException e) { ise2Thrown = true; }
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
        printBag("After add(null), add(null), add(\"real\")", bag);

        check("size is 3 — two nulls plus one real item",  bag.size() == 3,      tally);
        check("contains(null) is true",                    bag.contains(null),   tally);
        check("contains(\"real\") true alongside nulls",   bag.contains("real"), tally);

        bag.remove(null); // only the FIRST null is removed
        printBag("After first remove(null)", bag);
        check("size is 2 after removing one null",     bag.size() == 2,    tally);
        check("contains(null) still true – one left",  bag.contains(null), tally);

        bag.remove(null);
        printBag("After second remove(null)", bag);
        check("contains(null) false — both nulls gone", !bag.contains(null), tally);

        // Iterator visits null element without crashing
        Bag<String> bag2 = new Bag<>();
        bag2.add("before");
        bag2.add(null);
        bag2.add("after");
        printBag("Bag with null at index 1", bag2);
        System.out.println("  Iterating (null rendered as <null>):");
        int idx = 0;
        for (String s : bag2) {
            System.out.printf("    cursor[%d] → %s%n", idx++,
                    s == null ? "<null>" : "\"" + s + "\"");
        }
        java.util.List<String> list = toList(bag2);
        check("iterator visits all 3 elements including null", list.size() == 3,    tally);
        check("null is at index 1 in iteration order",         list.get(1) == null, tally);
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
        printBag("After adding three \"dup\" entries", bag);

        check("size is 3 — three copies present", bag.size() == 3,     tally);
        check("contains(\"dup\") is true",         bag.contains("dup"), tally);

        bag.remove("dup"); // removes index 0 only
        printBag("After remove(\"dup\") — only FIRST occurrence removed", bag);
        check("size is 2 after removing one duplicate",     bag.size() == 2,     tally);
        check("contains(\"dup\") still true – copies left", bag.contains("dup"), tally);

        System.out.println("  Iterating over remaining duplicates:");
        int count = 0;
        int idx = 0;
        for (String s : bag) {
            System.out.printf("    cursor[%d] → \"%s\"%n", idx++, s);
            if ("dup".equals(s)) count++;
        }
        check("iterator sees exactly 2 remaining duplicates", count == 2, tally);

        bag.remove("dup");
        printBag("After second remove(\"dup\")", bag);
        bag.remove("dup");
        printBag("After third remove(\"dup\") — Bag now empty", bag);
        check("Bag is empty after all duplicates removed", bag.isEmpty(), tally);
    }

    // ==================================================================
    // 11. Generic type flexibility
    // ==================================================================

    static void smokeGenericTypes(int[] tally) {
        printHeader("11. Generic Type Flexibility");

        // Bag<Integer>
        Bag<Integer> intBag = new Bag<>();
        for (int i = 1; i <= 5; i++) intBag.add(i);
        printBag("Bag<Integer> [1..5]", intBag);
        int sum = 0;
        for (int n : intBag) sum += n;
        check("Bag<Integer> sums 1–5 correctly (expected 15)", sum == 15, tally);

        // Bag<Double>
        Bag<Double> dblBag = new Bag<>();
        dblBag.add(1.5);
        dblBag.add(2.5);
        printBag("Bag<Double>", dblBag);
        check("Bag<Double> contains 1.5", dblBag.contains(1.5), tally);

        // Bag<int[]>
        Bag<int[]> arrBag = new Bag<>();
        int[] arr = {10, 20, 30};
        arrBag.add(arr);
        printBag("Bag<int[]>", arrBag);
        check("Bag<int[]> contains the added array (reference equality)", arrBag.contains(arr), tally);

        // Bag<Object> – mixed runtime types
        Bag<Object> mixed = new Bag<>();
        mixed.add("string");
        mixed.add(99);
        mixed.add(3.14);
        mixed.add(null);
        printBag("Bag<Object> — mixed types (String, Integer, Double, null)", mixed);
        check("Bag<Object> size is 4",             mixed.size() == 4,        tally);
        check("Bag<Object> contains \"string\"",   mixed.contains("string"), tally);
        check("Bag<Object> contains 99",           mixed.contains(99),       tally);
        check("Bag<Object> contains null",         mixed.contains(null),     tally);
    }

    // ==================================================================
    // Visual helper – printBag()
    // ==================================================================

    /**
     * Renders the current contents of any Bag as a labelled ASCII
     * ArrayList diagram, printed to stdout.
     *
     * Non-empty example (CELL_WIDTH = 10):
     *
     *   ArrayList  size=3   [After add("gamma")]
     *   ┌────────────┬────────────┬────────────┐
     *   │ [0]        │ [1]        │ [2]        │
     *   │ alpha      │ beta       │ gamma      │
     *   └────────────┴────────────┴────────────┘
     *
     * Empty example:
     *
     *   ArrayList  size=0   [Initial state]
     *   ┌──────────────────────┐
     *   │       (empty)        │
     *   └──────────────────────┘
     *
     * @param label short description rendered next to "ArrayList  size=N"
     * @param bag   the Bag to visualise (any element type E)
     */
    private static <T> void printBag(String label, Bag<T> bag) {
        final int CELL = 10;   // inner character width of each cell

        java.util.List<T> elements = toList(bag);
        int size = elements.size();

        System.out.printf("%n  ArrayList  size=%-3d  [%s]%n", size, label);

        if (size == 0) {
            int width = CELL + 4;   // 2 border chars + 2 padding
            System.out.println("  ┌" + "─".repeat(width) + "┐");
            System.out.printf( "  │ %-" + (width) + "s│%n", "    (empty)");
            System.out.println("  └" + "─".repeat(width) + "┘");
            return;
        }

        // Build display strings for index and value rows
        String[] idxLabels = new String[size];
        String[] valLabels = new String[size];
        for (int i = 0; i < size; i++) {
            idxLabels[i] = fit("[" + i + "]",                   CELL);
            valLabels[i] = fit(renderValue(elements.get(i)),    CELL);
        }

        // ┌──────────────┬──────────────┐
        StringBuilder top = new StringBuilder("  ┌");
        for (int i = 0; i < size; i++) {
            top.append("─".repeat(CELL + 2));
            top.append(i < size - 1 ? "┬" : "┐");
        }
        // │ [0]        │ [1]        │
        StringBuilder idxRow = new StringBuilder("  │");
        for (String s : idxLabels) idxRow.append(" ").append(s).append(" │");
        // │ alpha      │ beta       │
        StringBuilder valRow = new StringBuilder("  │");
        for (String s : valLabels) valRow.append(" ").append(s).append(" │");
        // └──────────────┴──────────────┘
        StringBuilder bot = new StringBuilder("  └");
        for (int i = 0; i < size; i++) {
            bot.append("─".repeat(CELL + 2));
            bot.append(i < size - 1 ? "┴" : "┘");
        }

        System.out.println(top);
        System.out.println(idxRow);
        System.out.println(valRow);
        System.out.println(bot);
    }

    /**
     * Returns a printable string for any element value.
     * <ul>
     *   <li>{@code null}   → {@code "<null>"}</li>
     *   <li>{@code int[]}  → {@code "[10,20,30]"}</li>
     *   <li>anything else  → {@code toString()}</li>
     * </ul>
     */
    private static String renderValue(Object value) {
        if (value == null) return "<null>";
        if (value instanceof int[]) {
            int[] a = (int[]) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) {
                sb.append(a[i]);
                if (i < a.length - 1) sb.append(",");
            }
            return sb.append("]").toString();
        }
        return value.toString();
    }

    /**
     * Fits {@code s} into exactly {@code width} characters:
     * pads with spaces when shorter, truncates with "…" when longer.
     */
    private static String fit(String s, int width) {
        if (s.length() > width) return s.substring(0, width - 1) + "…";
        return String.format("%-" + width + "s", s);
    }

    // ==================================================================
    // Utility helpers
    // ==================================================================

    /** Prints a framed section header. */
    private static void printHeader(String title) {
        System.out.println("\n┌─────────────────────────────────────────────────┐");
        System.out.printf( "│  %-47s│%n", title);
        System.out.println("└─────────────────────────────────────────────────┘");
    }

    /**
     * Evaluates {@code condition} and prints PASS / FAIL.
     * Increments tally[0] on pass, tally[1] on fail.
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

    /** Drains any Iterable into a new ArrayList. */
    private static <T> java.util.List<T> toList(Iterable<T> iterable) {
        java.util.List<T> list = new java.util.ArrayList<>();
        for (T item : iterable) list.add(item);
        return list;
    }
}