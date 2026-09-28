package org.hhuop.experiment;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Assign a seeded quantity and unit-profit realization to a binary transaction file. */
public final class UtilityRealizationMain {
    public static void main(String[] argv) throws Exception {
        Args a = new Args(argv);
        Path input = Path.of(a.get("input", "binary.txt"));
        Path out = Path.of(a.get("outDir", "datasets/realization"));
        long seed = a.getLong("seed", 701);
        int maxQty = a.getInt("maxQty", 10);
        int maxProfit = a.getInt("maxProfit", 10);
        if (maxQty < 1 || maxProfit < 1) throw new IllegalArgumentException("maxQty and maxProfit must be >= 1");
        List<List<String>> transactions = new ArrayList<>();
        Set<String> items = new TreeSet<>();
        for (String raw : Files.readAllLines(input)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("%") || line.startsWith("@")) continue;
            List<String> tx = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String id : line.split("\\s+")) {
                if (id.isBlank() || id.matches("T\\d+")) continue;
                String item = id.contains(":") ? id.substring(0, id.indexOf(':')) : (id.startsWith("i") ? id : "i" + id);
                if (!seen.add(item)) throw new IOException("Duplicate item " + item);
                tx.add(item);
                items.add(item);
            }
            if (!tx.isEmpty()) transactions.add(tx);
        }
        Random random = new Random(seed);
        Map<String,Integer> profit = new LinkedHashMap<>();
        for (String item : items) profit.put(item, 1 + random.nextInt(maxProfit));
        Files.createDirectories(out);
        List<String> db = new ArrayList<>();
        for (int tid = 0; tid < transactions.size(); tid++) {
            StringBuilder row = new StringBuilder("T").append(tid);
            for (String item : transactions.get(tid)) row.append(' ').append(item).append(':').append(1 + random.nextInt(maxQty));
            db.add(row.toString());
        }
        List<String> profits = new ArrayList<>();
        for (var e : profit.entrySet()) profits.add(e.getKey() + " " + e.getValue());
        Files.write(out.resolve("database.txt"), db);
        Files.write(out.resolve("profits.txt"), profits);
        Files.writeString(out.resolve("SOURCE.txt"),
                "Binary source: " + input.toAbsolutePath() + System.lineSeparator()
                        + "seed=" + seed + " quantity=Uniform{1.." + maxQty + "} profit=Uniform{1.." + maxProfit + "}" + System.lineSeparator()
                        + "The transaction membership is unchanged. One realization is shared by every algorithm." + System.lineSeparator());
        System.out.printf(Locale.ROOT, "realization transactions=%d items=%d seed=%d dir=%s%n",
                transactions.size(), items.size(), seed, out.toAbsolutePath());
    }
}
