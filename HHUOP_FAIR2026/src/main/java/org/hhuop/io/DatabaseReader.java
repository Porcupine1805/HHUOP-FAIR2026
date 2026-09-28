package org.hhuop.io;

import org.hhuop.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Reader for the project format.
 * profits: item whitespace profit, e.g. a 4
 * database: TID item:quantity item:quantity ..., e.g. T0 a:10 b:1 c:3
 */
public final class DatabaseReader {
    private DatabaseReader() {}

    public static QuantitativeDatabase read(Path dbFile, Path profitFile) throws IOException {
        Map<String,Double> profits = new LinkedHashMap<>();
        int lineNo=0;
        try (BufferedReader reader=Files.newBufferedReader(profitFile)) { for (String raw; (raw=reader.readLine())!=null;) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("[\\s,;]+");
            if (p.length != 2) throw new IOException(profitFile+":"+lineNo+": bad profit line: " + line);
            double value=parseDouble(p[1],profitFile,lineNo,"profit");
            if(!Double.isFinite(value)||value<=0) throw new IOException(profitFile+":"+lineNo+": profit must be finite and > 0");
            if(profits.putIfAbsent(p[0],value)!=null) throw new IOException(profitFile+":"+lineNo+": duplicate profit for "+p[0]);
        }}
        if(profits.isEmpty()) throw new IOException(profitFile+": no profits found");
        List<Transaction> txs = new ArrayList<>();
        int fallbackTid = 0;
        Set<Integer> tids=new HashSet<>(); lineNo=0;
        try (BufferedReader reader=Files.newBufferedReader(dbFile)) { for (String raw; (raw=reader.readLine())!=null;) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            int tid;
            int start;
            if (p[0].matches("[Tt]\\d+")) { tid = Integer.parseInt(p[0].substring(1)); start = 1; }
            else if (p[0].matches("\\d+") && !p[0].contains(":")) { tid = Integer.parseInt(p[0]); start = 1; }
            else { tid = fallbackTid; start = 0; }
            fallbackTid = Math.max(fallbackTid + 1, tid + 1);
            Map<String,Integer> q = new LinkedHashMap<>();
            for (int i=start;i<p.length;i++) {
                String[] kv = p[i].split(":");
                if (kv.length != 2) throw new IOException(dbFile+":"+lineNo+": bad transaction token: " + p[i]);
                String item = kv[0]; int qty=parseInt(kv[1],dbFile,lineNo,"quantity");
                if (!profits.containsKey(item)) throw new IOException(dbFile+":"+lineNo+": missing profit for item " + item);
                if(qty<=0) throw new IOException(dbFile+":"+lineNo+": quantity must be > 0 for "+item);
                if(q.putIfAbsent(item,qty)!=null) throw new IOException(dbFile+":"+lineNo+": duplicate item "+item);
            }
            if(!tids.add(tid)) throw new IOException(dbFile+":"+lineNo+": duplicate TID "+tid);
            if(q.isEmpty()) throw new IOException(dbFile+":"+lineNo+": empty transaction");
            txs.add(new Transaction(tid, q));
        }}
        if(txs.isEmpty()) throw new IOException(dbFile+": no transactions found");
        return new QuantitativeDatabase(txs, profits);
    }

    private static int parseInt(String s,Path file,int line,String what)throws IOException{
        try{return Integer.parseInt(s);}catch(NumberFormatException e){throw new IOException(file+":"+line+": invalid "+what+" '"+s+"'",e);}
    }
    private static double parseDouble(String s,Path file,int line,String what)throws IOException{
        try{return Double.parseDouble(s);}catch(NumberFormatException e){throw new IOException(file+":"+line+": invalid "+what+" '"+s+"'",e);}
    }

    public static Set<Itemset> readItemsets(Path file) throws IOException {
        Set<Itemset> result = new LinkedHashSet<>();
        for (String raw: Files.readAllLines(file)) {
            String line=raw.trim(); if(line.isEmpty()||line.startsWith("#")) continue;
            String[] items = line.replace("{","").replace("}","").split("[,\\s]+");
            result.add(new Itemset(Arrays.asList(items)));
        }
        return result;
    }
}
