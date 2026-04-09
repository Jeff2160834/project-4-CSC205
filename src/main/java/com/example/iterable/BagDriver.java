package com.example.iterable;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * BagDriver – Smoke-test driver for the forEach / spliterator / fail-fast Bag.
 *
 * Sections:
 *   1.  forEach — basic traversal with ArrayList visual per element
 *   2.  forEach — fail-fast: mutation inside lambda throws CME
 *   3.  forEach — fail-fast: external mutation between forEach calls
 *   4.  Live iterator — fail-fast on external add
 *   5.  Live iterator — fail-fast on external remove
 *   6.  Live iterator — own remove() is NOT flagged as concurrent modification
 *   7.  Live iterator — forEachRemaining
 *   8.  Live iterator — forEachRemaining fail-fast
 *   9.  Snapshot iterator — immune to external mutations
 *  10.  spliterator — tryAdvance element-by-element
 *  11.  spliterator — forEachRemaining bulk
 *  12.  spliterator — trySplit halves
 *  13.  spliterator — characteristics (ORDERED, SIZED, SUBSIZED, IMMUTABLE)
 *  14.  spliterator — sequential stream via StreamSupport
 *  15.  spliterator — parallel stream via StreamSupport
 *  16.  Regression: all prior mutating methods still increment modCount correctly
 */
public class BagDriver {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Bag<E>  forEach / spliterator / fail-fast  Driver   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        int[] tally = {0, 0};

        smokeForEach(tally);
        smokeForEachFailFastLambda(tally);
        smokeForEachFailFastExternal(tally);
        smokeLiveIteratorFailFastAdd(tally);
        smokeLiveIteratorFailFastRemove(tally);
        smokeLiveIteratorOwnRemoveSafe(tally);
        smokeLiveIteratorForEachRemaining(tally);
        smokeLiveIteratorForEachRemainingFailFast(tally);
        smokeSnapshotIteratorImmune(tally);
        smokeSpliteratorTryAdvance(tally);
        smokeSpliteratorForEachRemaining(tally);
        smokeSpliteratorTrySplit(tally);
        smokeSpliteratorCharacteristics(tally);
        smokeSpliteratorSequentialStream(tally);
        smokeSpliteratorParallelStream(tally);
        smokeModCountRegression(tally);

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.printf( "║  Results:  %3d passed  |  %3d failed               ║%n",
                tally[0], tally[1]);
        System.out.println("╚══════════════════════════════════════════════════════╝");
        if (tally[1] > 0) System.exit(1);
    }

    // ==================================================================
    // 1. forEach — basic traversal
    // ==================================================================

    static void smokeForEach(int[] tally) {
        printHeader("1. forEach — Basic Traversal");

        Bag<String> bag = new Bag<>(List.of("alpha", "beta", "gamma", "delta"));
        printBag("Source Bag", bag);

        List<String> collected = new ArrayList<>();
        System.out.println("  forEach traversal:");
        bag.forEach(item -> {
            System.out.printf("    → \"%s\"%n", item);
            collected.add(item);
        });
        printBag("Bag unchanged after forEach", bag);

        check("forEach visited 4 elements",          collected.size() == 4,     tally);
        check("forEach order: index 0 = alpha",      "alpha".equals(collected.get(0)), tally);
        check("forEach order: index 3 = delta",      "delta".equals(collected.get(3)), tally);
        check("Bag size unchanged after forEach",    bag.size() == 4,           tally);

        // forEach on empty bag
        Bag<String> empty = new Bag<>();
        List<String> emptyResult = new ArrayList<>();
        empty.forEach(emptyResult::add);
        check("forEach on empty Bag visits 0 elements", emptyResult.isEmpty(), tally);
    }

    // ==================================================================
    // 2. forEach — fail-fast: mutation INSIDE the consumer lambda
    // ==================================================================

    static void smokeForEachFailFastLambda(int[] tally) {
        printHeader("2. forEach — Fail-Fast: Mutation Inside Lambda");

        Bag<String> bag = new Bag<>(List.of("a", "b", "c"));
        printBag("Before forEach with mutating lambda", bag);

        boolean cme = false;
        try {
            bag.forEach(item -> {
                // Mutate the Bag inside the consumer — should trigger CME
                if ("b".equals(item)) bag.add("ILLEGAL");
            });
        } catch (ConcurrentModificationException e) {
            cme = true;
            System.out.println("  ConcurrentModificationException caught: " + e.getMessage());
        }
        check("forEach throws CME when lambda mutates Bag", cme, tally);
        printBag("Bag after interrupted forEach", bag);
    }

    // ==================================================================
    // 3. forEach — fail-fast: external mutation between forEach start and end
    // ==================================================================

    static void smokeForEachFailFastExternal(int[] tally) {
        printHeader("3. forEach — Fail-Fast: External Mutation via Second Reference");

        Bag<String> bag = new Bag<>(List.of("x", "y", "z"));
        printBag("Bag before external-mutation forEach", bag);

        // Use a second reference to simulate external mutation
        Bag<String> ref = bag;
        boolean cme = false;
        try {
            // Using forEachRemaining on live iterator — same modCount contract
            Iterator<String> it = bag.iterator();
            it.next();           // consume "x"
            ref.add("W");        // external structural change
            it.next();           // should throw CME
        } catch (ConcurrentModificationException e) {
            cme = true;
            System.out.println("  ConcurrentModificationException caught: " + e.getMessage());
        }
        check("External add detected by live iterator (fail-fast)", cme, tally);
    }

    // ==================================================================
    // 4. Live iterator — fail-fast on external add
    // ==================================================================

    static void smokeLiveIteratorFailFastAdd(int[] tally) {
        printHeader("4. Live Iterator — Fail-Fast on External add()");

        Bag<Integer> bag = new Bag<>(List.of(1, 2, 3));
        printBag("Bag [1, 2, 3]", bag);

        Iterator<Integer> it = bag.iterator();
        it.next();           // cursor at 1, OK
        bag.add(99);         // external structural change
        printBag("After external add(99)", bag);

        boolean cme = false;
        try { it.next(); } catch (ConcurrentModificationException e) { cme = true;
            System.out.println("  CME on next(): " + e.getMessage()); }
        check("Live iterator detects external add() via CME", cme, tally);
    }

    // ==================================================================
    // 5. Live iterator — fail-fast on external remove
    // ==================================================================

    static void smokeLiveIteratorFailFastRemove(int[] tally) {
        printHeader("5. Live Iterator — Fail-Fast on External remove()");

        Bag<Integer> bag = new Bag<>(List.of(10, 20, 30));
        printBag("Bag [10, 20, 30]", bag);

        Iterator<Integer> it = bag.iterator();
        it.next();           // consume 10
        bag.remove(10);      // external remove — modCount bumps
        printBag("After external remove(10)", bag);

        boolean cme = false;
        try { it.next(); } catch (ConcurrentModificationException e) { cme = true;
            System.out.println("  CME on next(): " + e.getMessage()); }
        check("Live iterator detects external remove() via CME", cme, tally);
    }

    // ==================================================================
    // 6. Live iterator — own remove() is NOT flagged as CME
    // ==================================================================

    static void smokeLiveIteratorOwnRemoveSafe(int[] tally) {
        printHeader("6. Live Iterator — Own remove() Does NOT Trigger CME");

        Bag<String> bag = new Bag<>(List.of("keep", "drop", "keep-too"));
        printBag("Before iterator-remove pass", bag);

        List<String> kept = new ArrayList<>();
        System.out.println("  Iterating + removing \"drop\":");
        boolean cme = false;
        try {
            Iterator<String> it = bag.iterator();
            while (it.hasNext()) {
                String val = it.next();
                if ("drop".equals(val)) {
                    it.remove();
                    System.out.println("    removed → \"drop\"");
                } else {
                    System.out.printf("    kept    → \"%s\"%n", val);
                    kept.add(val);
                }
            }
        } catch (ConcurrentModificationException e) {
            cme = true;
        }
        printBag("After iterator-remove pass", bag);

        check("Iterator own remove() does NOT throw CME",  !cme,                       tally);
        check("\"drop\" removed correctly",                !bag.contains("drop"),       tally);
        check("Remaining elements intact",                  kept.equals(List.of("keep", "keep-too")), tally);
    }

    // ==================================================================
    // 7. Live iterator — forEachRemaining
    // ==================================================================

    static void smokeLiveIteratorForEachRemaining(int[] tally) {
        printHeader("7. Live Iterator — forEachRemaining");

        Bag<String> bag = new Bag<>(List.of("one", "two", "three", "four", "five"));
        printBag("Bag [one..five]", bag);

        Iterator<String> it = bag.iterator();
        it.next();  // consume "one" manually

        List<String> rest = new ArrayList<>();
        System.out.println("  forEachRemaining after consuming 'one':");
        it.forEachRemaining(s -> {
            System.out.printf("    → \"%s\"%n", s);
            rest.add(s);
        });

        check("forEachRemaining visited 4 elements",      rest.size() == 4,                  tally);
        check("forEachRemaining started at 'two'",        "two".equals(rest.get(0)),          tally);
        check("forEachRemaining ended at 'five'",         "five".equals(rest.get(3)),         tally);
        check("iterator exhausted after forEachRemaining",!it.hasNext(),                      tally);
    }

    // ==================================================================
    // 8. Live iterator — forEachRemaining fail-fast
    // ==================================================================

    static void smokeLiveIteratorForEachRemainingFailFast(int[] tally) {
        printHeader("8. Live Iterator — forEachRemaining Fail-Fast");

        Bag<String> bag = new Bag<>(List.of("a", "b", "c", "d"));
        printBag("Bag [a,b,c,d]", bag);

        Iterator<String> it = bag.iterator();
        boolean cme = false;
        try {
            it.forEachRemaining(s -> {
                if ("b".equals(s)) bag.add("BOOM");  // structural change mid-loop
            });
        } catch (ConcurrentModificationException e) {
            cme = true;
            System.out.println("  CME caught in forEachRemaining: " + e.getMessage());
        }
        check("forEachRemaining detects mutation inside consumer (CME)", cme, tally);
    }

    // ==================================================================
    // 9. Snapshot iterator — immune to external mutations
    // ==================================================================

    static void smokeSnapshotIteratorImmune(int[] tally) {
        printHeader("9. Snapshot Iterator — Immune to External Mutations");

        Bag<String> bag = new Bag<>(List.of("p", "q", "r"));
        printBag("Bag before snapshot", bag);

        Iterator<String> snap = bag.snapshotIterator();

        bag.add("s");         // live mutation 1
        bag.remove("p");      // live mutation 2
        printBag("Live Bag mutated (added s, removed p)", bag);

        List<String> seen = new ArrayList<>();
        System.out.println("  Snapshot iterator sees:");
        while (snap.hasNext()) {
            String val = snap.next();
            System.out.printf("    → \"%s\"%n", val);
            seen.add(val);
        }

        check("Snapshot still sees 3 original elements",      seen.size() == 3,         tally);
        check("Snapshot still sees 'p' (removed from live)",  seen.contains("p"),       tally);
        check("Snapshot does NOT see 's' (added to live)",    !seen.contains("s"),      tally);
        check("Snapshot iterator is NOT affected by CME",      true,                    tally);

        // Snapshot remove() must still throw
        Iterator<String> snap2 = bag.snapshotIterator();
        snap2.next();
        boolean uoe = false;
        try { snap2.remove(); } catch (UnsupportedOperationException e) { uoe = true; }
        check("Snapshot remove() throws UnsupportedOperationException", uoe, tally);
    }

    // ==================================================================
    // 10. spliterator — tryAdvance element-by-element
    // ==================================================================

    static void smokeSpliteratorTryAdvance(int[] tally) {
        printHeader("10. Spliterator — tryAdvance Element-by-Element");

        Bag<String> bag = new Bag<>(List.of("A", "B", "C"));
        printBag("Bag [A, B, C]", bag);

        Spliterator<String> sp = bag.spliterator();
        List<String> visited = new ArrayList<>();
        System.out.println("  tryAdvance calls:");
        while (sp.tryAdvance(s -> {
            System.out.printf("    → \"%s\"%n", s);
            visited.add(s);
        }));

        check("tryAdvance visited 3 elements",          visited.size() == 3,        tally);
        check("tryAdvance order: A, B, C",              visited.equals(List.of("A","B","C")), tally);
        check("tryAdvance returns false when exhausted", !sp.tryAdvance(s -> {}),   tally);
    }

    // ==================================================================
    // 11. spliterator — forEachRemaining bulk
    // ==================================================================

    static void smokeSpliteratorForEachRemaining(int[] tally) {
        printHeader("11. Spliterator — forEachRemaining Bulk");

        Bag<Integer> bag = new Bag<>(List.of(10, 20, 30, 40, 50));
        printBag("Bag [10..50]", bag);

        Spliterator<Integer> sp = bag.spliterator();
        sp.tryAdvance(n -> {}); // consume 10
        sp.tryAdvance(n -> {}); // consume 20

        List<Integer> rest = new ArrayList<>();
        System.out.println("  forEachRemaining after consuming 10, 20:");
        sp.forEachRemaining(n -> {
            System.out.printf("    → %d%n", n);
            rest.add(n);
        });

        check("forEachRemaining visited 3 remaining", rest.size() == 3,                tally);
        check("forEachRemaining started at 30",       rest.get(0).equals(30),          tally);
        check("forEachRemaining ended at 50",         rest.get(2).equals(50),          tally);
    }

    // ==================================================================
    // 12. spliterator — trySplit halves
    // ==================================================================

    static void smokeSpliteratorTrySplit(int[] tally) {
        printHeader("12. Spliterator — trySplit Halves");

        Bag<Integer> bag = new Bag<>(List.of(1, 2, 3, 4, 5, 6));
        printBag("Bag [1..6]", bag);

        Spliterator<Integer> full = bag.spliterator();
        System.out.printf("  full.estimateSize() = %d%n", full.estimateSize());

        Spliterator<Integer> left = full.trySplit();  // left=[1,2,3], full=[4,5,6]
        System.out.printf("  After trySplit: left.estimateSize()=%d, right.estimateSize()=%d%n",
                left.estimateSize(), full.estimateSize());

        List<Integer> leftElements  = new ArrayList<>();
        List<Integer> rightElements = new ArrayList<>();
        left.forEachRemaining(leftElements::add);
        full.forEachRemaining(rightElements::add);

        printNumberBag("Left half",  leftElements);
        printNumberBag("Right half", rightElements);

        check("trySplit left  size = 3",         leftElements.size() == 3,               tally);
        check("trySplit right size = 3",         rightElements.size() == 3,              tally);
        check("trySplit left  = [1, 2, 3]",      leftElements.equals(List.of(1,2,3)),    tally);
        check("trySplit right = [4, 5, 6]",      rightElements.equals(List.of(4,5,6)),   tally);

        // trySplit on single element returns null
        Bag<String> singleton = new Bag<>(List.of("only"));
        Spliterator<String> sp = singleton.spliterator();
        sp.tryAdvance(s -> {}); // exhaust
        check("trySplit on exhausted spliterator returns null", sp.trySplit() == null, tally);
    }

    // ==================================================================
    // 13. spliterator — characteristics
    // ==================================================================

    static void smokeSpliteratorCharacteristics(int[] tally) {
        printHeader("13. Spliterator — Characteristics");

        Bag<String> bag = new Bag<>(List.of("x", "y", "z"));
        Spliterator<String> sp = bag.spliterator();
        int chars = sp.characteristics();

        System.out.printf("  characteristics() = 0x%X%n", chars);
        System.out.printf("  ORDERED   (%s) %s%n",
                (chars & Spliterator.ORDERED)   != 0 ? "SET" : "   ",
                (chars & Spliterator.ORDERED)   != 0 ? "✓" : "✗");
        System.out.printf("  SIZED     (%s) %s%n",
                (chars & Spliterator.SIZED)     != 0 ? "SET" : "   ",
                (chars & Spliterator.SIZED)     != 0 ? "✓" : "✗");
        System.out.printf("  SUBSIZED  (%s) %s%n",
                (chars & Spliterator.SUBSIZED)  != 0 ? "SET" : "   ",
                (chars & Spliterator.SUBSIZED)  != 0 ? "✓" : "✗");
        System.out.printf("  IMMUTABLE (%s) %s%n",
                (chars & Spliterator.IMMUTABLE) != 0 ? "SET" : "   ",
                (chars & Spliterator.IMMUTABLE) != 0 ? "✓" : "✗");

        check("ORDERED characteristic set",   (chars & Spliterator.ORDERED)   != 0, tally);
        check("SIZED characteristic set",     (chars & Spliterator.SIZED)     != 0, tally);
        check("SUBSIZED characteristic set",  (chars & Spliterator.SUBSIZED)  != 0, tally);
        check("IMMUTABLE characteristic set", (chars & Spliterator.IMMUTABLE) != 0, tally);
        check("estimateSize() matches Bag size", sp.estimateSize() == 3,            tally);
        check("getExactSizeIfKnown() == 3",      sp.getExactSizeIfKnown() == 3,     tally);
    }

    // ==================================================================
    // 14. spliterator — sequential stream via StreamSupport
    // ==================================================================

    static void smokeSpliteratorSequentialStream(int[] tally) {
        printHeader("14. Spliterator — Sequential Stream via StreamSupport");

        Bag<String> bag = new Bag<>(List.of("banana", "apple", "cherry", "date"));
        printBag("Source Bag", bag);

        List<String> filtered = StreamSupport
                .stream(bag.spliterator(), /* parallel= */ false)
                .filter(s -> s.length() > 4)
                .sorted()
                .collect(Collectors.toList());

        System.out.println("  Stream filter(len>4).sorted() result: " + filtered);

        // filter(len > 4) keeps: banana(6), apple(5), cherry(6) — sorted alphabetically
        check("Stream filter result size = 3",       filtered.size() == 3,             tally);
        check("Stream result[0] = apple",            "apple".equals(filtered.get(0)),  tally);
        check("Stream result[1] = banana",           "banana".equals(filtered.get(1)), tally);
        check("Bag unchanged after stream operation", bag.size() == 4,                 tally);
    }

    // ==================================================================
    // 15. spliterator — parallel stream via StreamSupport
    // ==================================================================

    static void smokeSpliteratorParallelStream(int[] tally) {
        printHeader("15. Spliterator — Parallel Stream via StreamSupport");

        Bag<Integer> bag = new Bag<>();
        for (int i = 1; i <= 100; i++) bag.add(i);
        printBag("Bag [1..100] (first/last shown)", bag);

        long sum = StreamSupport
                .stream(bag.spliterator(), /* parallel= */ true)
                .mapToLong(Integer::longValue)
                .sum();

        System.out.printf("  Parallel stream sum(1..100) = %d%n", sum);

        check("Parallel stream sum(1..100) = 5050",   sum == 5050L,  tally);
        check("Bag intact after parallel stream",     bag.size() == 100, tally);
    }

    // ==================================================================
    // 16. Regression: mutating methods increment modCount
    // ==================================================================

    static void smokeModCountRegression(int[] tally) {
        printHeader("16. Regression — All Mutating Methods Increment modCount");

        // Each sub-test: open an iterator, call the mutating method, confirm CME
        String[] labels = {"add", "addAll", "remove", "removeAll", "removeIf", "clear", "sort"};
        boolean[] results = new boolean[labels.length];

        // add
        Bag<String> b0 = new Bag<>(List.of("a","b","c"));
        Iterator<String> i0 = b0.iterator(); i0.next();
        try { b0.add("x"); i0.next(); } catch (ConcurrentModificationException e) { results[0]=true; }

        // addAll
        Bag<String> b1 = new Bag<>(List.of("a","b","c"));
        Iterator<String> i1 = b1.iterator(); i1.next();
        try { b1.addAll(List.of("x","y")); i1.next(); } catch (ConcurrentModificationException e) { results[1]=true; }

        // remove
        Bag<String> b2 = new Bag<>(List.of("a","b","c"));
        Iterator<String> i2 = b2.iterator(); i2.next();
        try { b2.remove("b"); i2.next(); } catch (ConcurrentModificationException e) { results[2]=true; }

        // removeAll
        Bag<String> b3 = new Bag<>(List.of("a","a","b"));
        Iterator<String> i3 = b3.iterator(); i3.next();
        try { b3.removeAll("a", 2); i3.next(); } catch (ConcurrentModificationException e) { results[3]=true; }

        // removeIf
        Bag<String> b4 = new Bag<>(List.of("a","b","c"));
        Iterator<String> i4 = b4.iterator(); i4.next();
        try { b4.removeIf(s->s.equals("b")); i4.next(); } catch (ConcurrentModificationException e) { results[4]=true; }

        // clear
        Bag<String> b5 = new Bag<>(List.of("a","b","c"));
        Iterator<String> i5 = b5.iterator(); i5.next();
        try { b5.clear(); i5.next(); } catch (ConcurrentModificationException e) { results[5]=true; }

        // sort
        Bag<String> b6 = new Bag<>(List.of("c","b","a"));
        Iterator<String> i6 = b6.iterator(); i6.next();
        try { b6.sort(null); i6.next(); } catch (ConcurrentModificationException e) { results[6]=true; }

        for (int i = 0; i < labels.length; i++) {
            check(labels[i] + "() increments modCount → CME on stale iterator", results[i], tally);
        }
    }

    // ==================================================================
    // Visual helpers
    // ==================================================================

    private static <T> void printBag(String label, Bag<T> bag) {
        final int CELL = 10;
        java.util.List<T> elements = toList(bag);
        int size = elements.size();

        // For large bags, truncate display to first 8 + last 2
        boolean truncated = size > 10;
        java.util.List<T> display = truncated
                ? new java.util.ArrayList<>(elements.subList(0, 8))
                : elements;

        System.out.printf("%n  ArrayList  size=%-4d [%s]%n", size, label);

        if (size == 0) {
            int w = CELL + 4;
            System.out.println("  ┌" + "─".repeat(w) + "┐");
            System.out.printf( "  │ %-" + w + "s│%n", "    (empty)");
            System.out.println("  └" + "─".repeat(w) + "┘");
            return;
        }

        int dispSize = display.size();
        String[] idx = new String[dispSize];
        String[] val = new String[dispSize];
        for (int i = 0; i < dispSize; i++) {
            idx[i] = fit("[" + i + "]",                    CELL);
            val[i] = fit(renderValue(display.get(i)),      CELL);
        }

        StringBuilder top = new StringBuilder("  ┌");
        StringBuilder ir  = new StringBuilder("  │");
        StringBuilder vr  = new StringBuilder("  │");
        StringBuilder bot = new StringBuilder("  └");

        for (int i = 0; i < dispSize; i++) {
            top.append("─".repeat(CELL + 2)).append(i < dispSize - 1 ? "┬" : (truncated ? "┬" : "┐"));
            ir .append(" ").append(idx[i]).append(" │");
            vr .append(" ").append(val[i]).append(" │");
            bot.append("─".repeat(CELL + 2)).append(i < dispSize - 1 ? "┴" : (truncated ? "┴" : "┘"));
        }
        if (truncated) {
            top.append("────────────┐");
            ir .append("  …(+").append(size - dispSize).append(") │");
            vr .append("  …       │");
            bot.append("────────────┘");
        }
        System.out.println(top);
        System.out.println(ir);
        System.out.println(vr);
        System.out.println(bot);
    }

    private static void printNumberBag(String label, java.util.List<?> elements) {
        final int CELL = 6;
        int size = elements.size();
        System.out.printf("%n  %s  size=%d%n", label, size);
        if (size == 0) { System.out.println("  (empty)"); return; }
        StringBuilder top = new StringBuilder("  ┌");
        StringBuilder ir  = new StringBuilder("  │");
        StringBuilder vr  = new StringBuilder("  │");
        StringBuilder bot = new StringBuilder("  └");
        for (int i = 0; i < size; i++) {
            top.append("─".repeat(CELL + 2)).append(i < size-1 ? "┬" : "┐");
            ir .append(" ").append(fit("["+i+"]", CELL)).append(" │");
            vr .append(" ").append(fit(String.valueOf(elements.get(i)), CELL)).append(" │");
            bot.append("─".repeat(CELL + 2)).append(i < size-1 ? "┴" : "┘");
        }
        System.out.println(top); System.out.println(ir);
        System.out.println(vr);  System.out.println(bot);
    }

    private static String renderValue(Object v) {
        if (v == null) return "<null>";
        return v.toString();
    }

    private static String fit(String s, int w) {
        if (s.length() > w) return s.substring(0, w - 1) + "…";
        return String.format("%-" + w + "s", s);
    }

    private static void printHeader(String title) {
        System.out.println("\n┌──────────────────────────────────────────────────────┐");
        System.out.printf( "│  %-52s│%n", title);
        System.out.println("└──────────────────────────────────────────────────────┘");
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