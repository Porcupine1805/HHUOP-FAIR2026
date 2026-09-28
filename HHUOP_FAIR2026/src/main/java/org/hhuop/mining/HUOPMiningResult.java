package org.hhuop.mining;

import org.hhuop.model.*;
import java.util.*;

public final class HUOPMiningResult {
    private final LinkedHashMap<Itemset,HUOPPattern> patterns;
    private final long visitedNodes;
    private final long runtimeNanos;

    public HUOPMiningResult(Map<Itemset,HUOPPattern> patterns, long visitedNodes, long runtimeNanos) {
        this.patterns = new LinkedHashMap<>();
        patterns.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> this.patterns.put(e.getKey(), e.getValue()));
        this.visitedNodes = visitedNodes; this.runtimeNanos = runtimeNanos;
    }
    public Map<Itemset,HUOPPattern> patterns() { return Collections.unmodifiableMap(patterns); }
    public Set<Itemset> itemsets() { return Collections.unmodifiableSet(patterns.keySet()); }
    public HUOPPattern get(Itemset x) { return patterns.get(x); }
    public long visitedNodes() { return visitedNodes; }
    public long runtimeNanos() { return runtimeNanos; }
    public double runtimeMs() { return runtimeNanos / 1_000_000.0; }
}
