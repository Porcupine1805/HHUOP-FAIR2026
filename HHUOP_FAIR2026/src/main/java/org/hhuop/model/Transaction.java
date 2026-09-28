package org.hhuop.model;

import java.util.*;

/** Quantitative transaction. Quantities are positive integers; absent items have quantity 0. */
public final class Transaction {
    private final int tid;
    private final LinkedHashMap<String,Integer> quantities;
    private transient Map<String,Double> cachedProfit;
    private transient double cachedTransactionUtility = Double.NaN;

    public Transaction(int tid, Map<String,Integer> quantities) {
        this.tid = tid;
        this.quantities = new LinkedHashMap<>();
        quantities.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> { if (e.getValue() > 0) this.quantities.put(e.getKey(), e.getValue()); });
    }
    public int tid() { return tid; }
    public Set<String> items() { return Collections.unmodifiableSet(quantities.keySet()); }
    public boolean contains(String item) { return quantities.containsKey(item); }
    public int quantity(String item) { return quantities.getOrDefault(item, 0); }
    public int itemCount() { return quantities.size(); }
    public Map<String,Integer> quantitiesView() { return Collections.unmodifiableMap(quantities); }

    public double utility(String item, Map<String,Double> profit) {
        return quantity(item) * profit.getOrDefault(item, 0.0);
    }
    public double utility(Itemset x, Map<String,Double> profit) {
        double s = 0;
        for (String i : x.items()) s += utility(i, profit);
        return s;
    }
    public double transactionUtility(Map<String,Double> profit) {
        if (profit == cachedProfit && !Double.isNaN(cachedTransactionUtility)) return cachedTransactionUtility;
        double s = 0;
        for (var e : quantities.entrySet()) s += e.getValue() * profit.getOrDefault(e.getKey(), 0.0);
        cachedProfit = profit;
        cachedTransactionUtility = s;
        return s;
    }
    public void reduce(String item, int k) {
        int q = quantity(item);
        if (q <= 0 || k <= 0 || k > q) throw new IllegalArgumentException("Invalid reduction for " + item + ": " + k);
        int nq = q - k;
        if (nq == 0) quantities.remove(item); else quantities.put(item, nq);
        cachedTransactionUtility = Double.NaN;
    }
    public void setQuantity(String item, int q) {
        if (q <= 0) quantities.remove(item); else quantities.put(item, q);
        cachedTransactionUtility = Double.NaN;
    }
    public Transaction copy() { return new Transaction(tid, quantities); }
    @Override public String toString() { return "T" + tid + quantities; }
}
