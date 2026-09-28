package org.hhuop.model;

import java.util.*;

/** Immutable itemset with canonical lexicographic item order. */
public final class Itemset implements Comparable<Itemset> {
    private final List<String> items;

    public Itemset(Collection<String> items) {
        TreeSet<String> sorted = new TreeSet<>(items);
        this.items = List.copyOf(sorted);
    }

    public static Itemset of(String... items) { return new Itemset(Arrays.asList(items)); }
    public List<String> items() { return items; }
    public int size() { return items.size(); }
    public boolean contains(String item) { return items.contains(item); }
    public boolean isSubsetOf(Transaction t) {
        for (String i : items) if (!t.contains(i)) return false;
        return true;
    }
    public String key() { return String.join(",", items); }

    @Override public int compareTo(Itemset o) {
        int n = Math.min(items.size(), o.items.size());
        for (int i = 0; i < n; i++) {
            int c = items.get(i).compareTo(o.items.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(items.size(), o.items.size());
    }
    @Override public boolean equals(Object o) { return o instanceof Itemset other && items.equals(other.items); }
    @Override public int hashCode() { return items.hashCode(); }
    @Override public String toString() { return "{" + String.join(",", items) + "}"; }
}
