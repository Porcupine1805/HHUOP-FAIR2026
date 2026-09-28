package org.hhuop.experiment;

import java.nio.file.*;
import java.util.*;

/**
 * Paired summaries for one result directory.
 * Blocks are seeds. The compared levels are one CSV column, usually algorithm or a weight.
 */
public final class RevisionStatsMain {
    public static void main(String[] argv) throws Exception {
        Args a = new Args(argv);
        Path dir = Path.of(a.get("inputDir", "results/revision/e1"));
        Path outDir = Path.of(a.get("outDir", "results/revision/e1_stats"));
        String factor = a.get("factor", "algorithm");
        String block = a.get("block", "seed");
        List<String> groupCols = List.of(a.get("groupBy", "dataset,alpha,beta,sensitiveFraction").split(","));
        List<String> metrics = List.of(a.get("metrics", "MC,AC,DUS,missingCount,artificialCount,actions,endToEndSanitizationMs,verificationMs,peakMemoryMB,peakRSS_MB").split(","));
        Map<String,String> hold = new LinkedHashMap<>();
        if (a.has("hold")) for (String token : a.get("hold", "").split(",")) {
            String[] kv = token.split("=", 2);
            if (kv.length == 2 && !kv[0].isBlank()) hold.put(kv[0].trim(), kv[1].trim());
        }
        int bootstrap = a.getInt("bootstrap", 10000);
        long bootSeed = a.getLong("bootSeed", 2026);
        List<Map<String,String>> rows = load(dir);
        rows.removeIf(r -> !matches(r, hold));
        Map<String,List<Map<String,String>>> groups = new LinkedHashMap<>();
        for (Map<String,String> row : rows) {
            String key = key(row, groupCols);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        Files.createDirectories(outDir);
        List<String> summary = new ArrayList<>();
        summary.add("group,factor,level,metric,n,mean,sd,ci95Low,ci95High");
        List<String> friedman = new ArrayList<>();
        friedman.add("group,metric,n,k,chiSquare,p,kendallW");
        List<String> pairs = new ArrayList<>();
        pairs.add("group,metric,levelA,levelB,n,meanDiff,diffCi95Low,diffCi95High,p,pHolm,rankBiserial");
        for (var g : groups.entrySet()) {
            for (String metric : metrics) {
                Map<String,Map<String,Double>> byLevel = new LinkedHashMap<>();
                for (Map<String,String> row : g.getValue()) {
                    if (!row.containsKey(factor) || !row.containsKey(block) || !row.containsKey(metric)) continue;
                    Double value = parse(row.get(metric));
                    if (value == null) continue;
                    byLevel.computeIfAbsent(row.get(factor), k -> new LinkedHashMap<>()).put(row.get(block), value);
                }
                if (byLevel.size() < 2) continue;
                List<String> levels = new ArrayList<>(byLevel.keySet());
                Collections.sort(levels);
                Set<String> common = null;
                for (String level : levels) {
                    Set<String> ids = byLevel.get(level).keySet();
                    if (common == null) common = new LinkedHashSet<>(ids);
                    else common.retainAll(ids);
                }
                List<String> blocks = new ArrayList<>(common);
                Collections.sort(blocks);
                int n = blocks.size();
                if (n < 2) continue;
                for (String level : levels) {
                    double[] xs = values(byLevel.get(level), blocks);
                    double mean = mean(xs), sd = sd(xs);
                    double half = tCritical(n - 1) * sd / Math.sqrt(n);
                    summary.add(csv(g.getKey(), factor, level, metric, n, mean, sd, mean - half, mean + half));
                }
                double[][] matrix = new double[n][levels.size()];
                for (int i = 0; i < n; i++) for (int j = 0; j < levels.size(); j++) matrix[i][j] = byLevel.get(levels.get(j)).get(blocks.get(i));
                FriedmanResult fr = friedman(matrix);
                friedman.add(String.join(",", esc(g.getKey()), metric, Integer.toString(n), Integer.toString(levels.size()),
                        num(fr.chi), num(fr.p), num(fr.w)));
                List<PairResult> tests = new ArrayList<>();
                for (int a1 = 0; a1 < levels.size(); a1++) for (int b1 = a1 + 1; b1 < levels.size(); b1++) {
                    double[] x = column(matrix, a1), y = column(matrix, b1);
                    tests.add(wilcoxon(g.getKey(), metric, levels.get(a1), levels.get(b1), x, y, bootstrap, bootSeed));
                }
                holm(tests);
                for (PairResult t : tests) pairs.add(t.toCsv());
            }
        }
        Files.write(outDir.resolve("summary_mean_ci.csv"), summary);
        Files.write(outDir.resolve("friedman.csv"), friedman);
        Files.write(outDir.resolve("pairwise_holm.csv"), pairs);
        System.out.printf(Locale.ROOT, "groups=%d summary=%d friedman=%d pairs=%d dir=%s%n",
                groups.size(), summary.size() - 1, friedman.size() - 1, pairs.size() - 1, outDir.toAbsolutePath());
    }

    private static List<Map<String,String>> load(Path dir) throws Exception {
        List<Map<String,String>> rows = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : stream.filter(Files::isRegularFile).filter(x -> x.toString().endsWith(".csv")).sorted().toList()) {
                String name = p.getFileName().toString();
                if (name.equals("aggregate.csv") || name.startsWith("summary") || name.startsWith("friedman") || name.startsWith("pairwise") || p.toString().contains("_actions")) continue;
                List<String> lines = Files.readAllLines(p);
                if (lines.size() < 2 || !lines.get(0).contains("algorithm")) continue;
                String[] header = splitCsv(lines.get(0));
                for (int i = 1; i < lines.size(); i++) {
                    if (lines.get(i).isBlank()) continue;
                    String[] cells = splitCsv(lines.get(i));
                    Map<String,String> row = new LinkedHashMap<>();
                    for (int c = 0; c < header.length && c < cells.length; c++) row.put(header[c], cells[c]);
                    String rss = rssFor(p);
                    if (rss != null) row.put("peakRSS_MB", rss);
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static String rssFor(Path csv) throws Exception {
        Path side = csv.resolveSibling(csv.getFileName().toString().replaceFirst("\\.csv$", ".rss"));
        if (!Files.isRegularFile(side)) return null;
        String text = Files.readString(side).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean matches(Map<String,String> row, Map<String,String> hold) {
        for (var e : hold.entrySet()) {
            String actual = row.get(e.getKey());
            if (actual == null) return false;
            Double left = parse(actual), right = parse(e.getValue());
            if (left != null && right != null) { if (Math.abs(left - right) > 1e-9) return false; }
            else if (!actual.equals(e.getValue())) return false;
        }
        return true;
    }

    private static String key(Map<String,String> row, List<String> cols) {
        List<String> parts = new ArrayList<>();
        for (String col : cols) parts.add(col + "=" + row.getOrDefault(col, ""));
        return String.join("|", parts);
    }

    private static double[] values(Map<String,Double> level, List<String> blocks) {
        double[] xs = new double[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) xs[i] = level.get(blocks.get(i));
        return xs;
    }

    private static double[] column(double[][] matrix, int j) {
        double[] xs = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) xs[i] = matrix[i][j];
        return xs;
    }

    private record FriedmanResult(double chi, double p, double w) {}
    private static FriedmanResult friedman(double[][] matrix) {
        int n = matrix.length, k = matrix[0].length;
        double[] rankSum = new double[k];
        double tie = 0;
        for (double[] row : matrix) {
            double[] ranks = ranks(row);
            for (int j = 0; j < k; j++) rankSum[j] += ranks[j];
            Map<Double,Integer> counts = new HashMap<>();
            for (double v : row) counts.merge(v, 1, Integer::sum);
            for (int t : counts.values()) if (t > 1) tie += (long) t * t * t - t;
        }
        double sumSq = 0;
        for (double r : rankSum) sumSq += r * r;
        double chi = 12.0 / (n * k * (k + 1.0)) * sumSq - 3.0 * n * (k + 1.0);
        double correction = 1 - tie / (n * k * (k * k - 1.0));
        if (correction > 1e-12) chi /= correction;
        double p = chiSquareSf(Math.max(0, chi), k - 1);
        double w = chi / (n * (k - 1.0));
        return new FriedmanResult(chi, p, w);
    }

    private static double[] ranks(double[] xs) {
        Integer[] order = new Integer[xs.length];
        for (int i = 0; i < xs.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> xs[i]));
        double[] rank = new double[xs.length];
        for (int i = 0; i < order.length;) {
            int j = i;
            while (j + 1 < order.length && xs[order[j + 1]] == xs[order[i]]) j++;
            double mid = (i + 1 + j + 1) / 2.0;
            for (int t = i; t <= j; t++) rank[order[t]] = mid;
            i = j + 1;
        }
        return rank;
    }

    private static final class PairResult {
        String group, metric, a, b; int n; double meanDiff, lo, hi, p, pHolm, r;
        String toCsv() { return String.join(",", esc(group), metric, esc(a), esc(b), Integer.toString(n), num(meanDiff), num(lo), num(hi), num(p), num(pHolm), num(r)); }
    }

    private static PairResult wilcoxon(String group, String metric, String a, String b, double[] x, double[] y, int bootstrap, long bootSeed) {
        int n = x.length;
        double[] d = new double[n];
        for (int i = 0; i < n; i++) d[i] = x[i] - y[i];
        List<Double> abs = new ArrayList<>();
        List<Integer> sign = new ArrayList<>();
        for (double v : d) if (v != 0) { abs.add(Math.abs(v)); sign.add(v > 0 ? 1 : -1); }
        PairResult t = new PairResult();
        t.group = group; t.metric = metric; t.a = a; t.b = b; t.n = n; t.meanDiff = mean(d);
        double[] boot = bootstrap(d, bootstrap, bootSeed + Objects.hash(group, metric, a, b));
        t.lo = boot[0]; t.hi = boot[1];
        int m = abs.size();
        if (m == 0) { t.p = 1; t.r = 0; return t; }
        double[] rank = ranks(abs.stream().mapToDouble(Double::doubleValue).toArray());
        double wPlus = 0, wMinus = 0;
        for (int i = 0; i < m; i++) { if (sign.get(i) > 0) wPlus += rank[i]; else wMinus += rank[i]; }
        t.r = (wPlus - wMinus) / (wPlus + wMinus);
        t.p = wilcoxonP(rank, wPlus);
        return t;
    }

    private static double wilcoxonP(double[] rank, double wPlus) {
        int m = rank.length;
        boolean integral = true;
        for (double r : rank) if (Math.abs(r - Math.rint(r)) > 1e-9) integral = false;
        if (integral && m <= 16) {
            double center = m * (m + 1) / 4.0;
            double dist = Math.abs(wPlus - center);
            int extreme = 0;
            int total = 1 << m;
            for (int mask = 0; mask < total; mask++) {
                double w = 0;
                for (int i = 0; i < m; i++) if ((mask & (1 << i)) != 0) w += rank[i];
                if (Math.abs(w - center) + 1e-9 >= dist) extreme++;
            }
            return extreme / (double) total;
        }
        double mean = m * (m + 1) / 4.0;
        double var = m * (m + 1.0) * (2.0 * m + 1.0) / 24.0;
        Map<Double,Integer> ties = new HashMap<>();
        for (double r : rank) ties.merge(r, 1, Integer::sum);
        for (int t : ties.values()) if (t > 1) var -= (t * t * t - t) / 48.0;
        if (var <= 0) return 1;
        double z = wPlus - mean;
        double cc = z == 0 ? 0 : 0.5 * Math.signum(z);
        z = (z - cc) / Math.sqrt(var);
        return Math.min(1, 2 * (1 - normCdf(Math.abs(z))));
    }

    private static void holm(List<PairResult> tests) {
        List<PairResult> ordered = new ArrayList<>(tests);
        ordered.sort(Comparator.comparingDouble(t -> t.p));
        int m = ordered.size();
        double running = 0;
        for (int i = 0; i < m; i++) {
            running = Math.max(running, Math.min(1, (m - i) * ordered.get(i).p));
            ordered.get(i).pHolm = running;
        }
    }

    private static double[] bootstrap(double[] d, int replicates, long seed) {
        Random random = new Random(seed);
        double[] means = new double[replicates];
        for (int r = 0; r < replicates; r++) {
            double s = 0;
            for (int i = 0; i < d.length; i++) s += d[random.nextInt(d.length)];
            means[r] = s / d.length;
        }
        Arrays.sort(means);
        int lo = (int) Math.floor(0.025 * (replicates - 1));
        int hi = (int) Math.ceil(0.975 * (replicates - 1));
        return new double[]{means[lo], means[hi]};
    }

    private static double mean(double[] xs) { double s = 0; for (double x : xs) s += x; return s / xs.length; }
    private static double sd(double[] xs) {
        if (xs.length < 2) return 0;
        double m = mean(xs), s = 0;
        for (double x : xs) s += (x - m) * (x - m);
        return Math.sqrt(s / (xs.length - 1));
    }
    private static double tCritical(int df) {
        if (df <= 0) return Double.NaN;
        double lo = 0, hi = 100;
        for (int i = 0; i < 80; i++) {
            double mid = (lo + hi) / 2;
            if (studentTwoTail(mid, df) > 0.05) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }
    private static double studentTwoTail(double t, int df) {
        double x = df / (df + t * t);
        return regularizedBeta(x, df / 2.0, 0.5);
    }
    private static double regularizedBeta(double x, double a, double b) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double bt = Math.exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * Math.log(x) + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) return bt * betaCF(x, a, b) / a;
        return 1 - bt * betaCF(1 - x, b, a) / b;
    }
    private static double betaCF(double x, double a, double b) {
        double qab = a + b, qap = a + 1, qam = a - 1;
        double c = 1, d = 1 - qab * x / qap;
        if (Math.abs(d) < 1e-30) d = 1e-30;
        d = 1 / d; double h = d;
        for (int m = 1; m <= 200; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d; if (Math.abs(d) < 1e-30) d = 1e-30;
            c = 1 + aa / c; if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1 / d; h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d; if (Math.abs(d) < 1e-30) d = 1e-30;
            c = 1 + aa / c; if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1 / d; double del = d * c; h *= del;
            if (Math.abs(del - 1) < 1e-12) break;
        }
        return h;
    }
    private static double chiSquareSf(double x, double df) { return gammaQ(df / 2.0, x / 2.0); }
    private static double gammaQ(double a, double x) {
        if (x <= 0) return 1;
        if (x < a + 1) return 1 - gammaPSeries(a, x);
        return gammaQCF(a, x);
    }
    private static double gammaPSeries(double a, double x) {
        double ap = a, sum = 1 / a, del = sum;
        for (int n = 0; n < 200; n++) { ap += 1; del *= x / ap; sum += del; if (Math.abs(del) < Math.abs(sum) * 1e-14) break; }
        return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
    }
    private static double gammaQCF(double a, double x) {
        double b = x + 1 - a, c = 1 / 1e-30, d = 1 / b, h = d;
        for (int i = 1; i <= 200; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b; if (Math.abs(d) < 1e-30) d = 1e-30;
            c = b + an / c; if (Math.abs(c) < 1e-30) c = 1e-30;
            d = 1 / d; double del = d * c; h *= del;
            if (Math.abs(del - 1) < 1e-12) break;
        }
        return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
    }
    private static double logGamma(double z) {
        double[] p = {0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
                -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (z < 0.5) return Math.log(Math.PI / Math.sin(Math.PI * z)) - logGamma(1 - z);
        z -= 1; double x = p[0];
        for (int i = 1; i < p.length; i++) x += p[i] / (z + i);
        double t = z + 7.5;
        return 0.5 * Math.log(2 * Math.PI) + (z + 0.5) * Math.log(t) - t + Math.log(x);
    }
    private static double normCdf(double z) {
        double t = 1 / (1 + 0.2316419 * Math.abs(z));
        double d = 0.3989422804014327 * Math.exp(-0.5 * z * z);
        double p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
        return z >= 0 ? 1 - p : p;
    }
    private static Double parse(String s) { try { if (s == null || s.isBlank() || s.equals("NaN")) return null; return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    private static String num(double x) { return String.format(Locale.US, "%.8g", x); }
    private static String csv(Object... xs) { String[] out = new String[xs.length]; for (int i = 0; i < xs.length; i++) out[i] = xs[i] instanceof String ? esc((String) xs[i]) : String.valueOf(xs[i]); return String.join(",", out); }
    private static String esc(String s) { return s.indexOf(',') >= 0 || s.indexOf('"') >= 0 ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
    private static String[] splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quote = false;
                else cur.append(c);
            } else if (c == '"') quote = true;
            else if (c == ',') { cells.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        cells.add(cur.toString());
        return cells.toArray(String[]::new);
    }
}
