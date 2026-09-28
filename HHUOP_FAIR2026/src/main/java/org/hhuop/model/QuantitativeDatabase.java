package org.hhuop.model;

import java.util.*;

public final class QuantitativeDatabase {
    private final List<Transaction> transactions;
    private final LinkedHashMap<String,Double> profits;
    private final Map<Integer,Transaction> byTid;
    private double totalUtility;

    public QuantitativeDatabase(List<Transaction> transactions, Map<String,Double> profits) {
        Objects.requireNonNull(transactions,"transactions"); Objects.requireNonNull(profits,"profits");
        if(transactions.isEmpty()) throw new IllegalArgumentException("Database must not be empty");
        for(var e:profits.entrySet()) if(e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||!Double.isFinite(e.getValue())||e.getValue()<=0)
            throw new IllegalArgumentException("Profits must be finite and > 0: "+e);
        this.transactions = new ArrayList<>(transactions);
        this.transactions.sort(Comparator.comparingInt(Transaction::tid));
        this.profits = new LinkedHashMap<>();
        profits.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> this.profits.put(e.getKey(), e.getValue()));
        this.byTid = new HashMap<>(Math.max(16, transactions.size()*2));
        for (Transaction t : this.transactions) {
            if (byTid.put(t.tid(), t) != null) throw new IllegalArgumentException("Duplicate TID " + t.tid());
            if(t.itemCount()==0) throw new IllegalArgumentException("Empty transaction T"+t.tid());
            for(String item:t.items()) if(!this.profits.containsKey(item)) throw new IllegalArgumentException("Missing profit for item "+item);
        }
        for (Transaction t : this.transactions) totalUtility += t.transactionUtility(this.profits);
    }
    public List<Transaction> transactions() { return Collections.unmodifiableList(transactions); }
    public Map<String,Double> profits() { return Collections.unmodifiableMap(profits); }
    public Transaction transactionByTid(int tid) {
        Transaction t=byTid.get(tid);
        if(t==null) throw new IllegalArgumentException("Unknown TID " + tid);
        return t;
    }
    public Set<String> allItems() { return Collections.unmodifiableSet(profits.keySet()); }
    public int size() { return transactions.size(); }
    public int minSupport(double alpha) { return (int)Math.ceil(alpha * size() - 1e-12); }
    public double totalUtility() { return totalUtility; }
    public void reduce(int tid,String item,int k){
        Transaction t=transactionByTid(tid); double loss=k*profits.get(item); t.reduce(item,k); totalUtility-=loss;
    }
    public long totalQuantity() { long s=0; for (Transaction t:transactions) for (int q:t.quantitiesView().values()) s += q; return s; }
    public long nonzeroCells() { long s=0; for (Transaction t:transactions) s += t.itemCount(); return s; }
    public QuantitativeDatabase copy() {
        List<Transaction> c = new ArrayList<>(); for (Transaction t:transactions) c.add(t.copy());
        return new QuantitativeDatabase(c, profits);
    }
}
