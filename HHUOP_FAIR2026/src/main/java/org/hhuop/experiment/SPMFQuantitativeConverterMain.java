package org.hhuop.experiment;

import java.nio.file.*;
import java.util.*;

/** Convert SPMF FHUQI/VHUQI quantitative format (item,quantity) + (item,profit) to project format. */
public final class SPMFQuantitativeConverterMain {
    public static void main(String[] argv) throws Exception {
        Args a=new Args(argv);
        Path dbIn=Path.of(a.get("dbIn","foodmart.txt"));
        Path profitIn=Path.of(a.get("profitIn","foodmartf1profit.txt"));
        Path outDir=Path.of(a.get("outDir","datasets/foodmart"));
        Files.createDirectories(outDir);
        List<String> dbOut=new ArrayList<>();
        int tid=0;
        for(String line:Files.readAllLines(dbIn)){
            line=line.trim(); if(line.isEmpty()||line.startsWith("#")||line.startsWith("%")||line.startsWith("@")) continue;
            StringBuilder b=new StringBuilder("T").append(tid++);
            for(String tok:line.split("\\s+")){
                if(tok.isBlank()) continue;
                String[] p=tok.split(",");
                if(p.length!=2) throw new IllegalArgumentException("Bad quantitative token: "+tok);
                b.append(' ').append(p[0].trim()).append(':').append(p[1].trim());
            }
            dbOut.add(b.toString());
        }
        List<String> profitOut=new ArrayList<>();
        for(String line:Files.readAllLines(profitIn)){
            line=line.trim(); if(line.isEmpty()||line.startsWith("#")||line.startsWith("%")||line.startsWith("@")) continue;
            String[] p=line.split(",");
            if(p.length!=2) throw new IllegalArgumentException("Bad profit line: "+line);
            profitOut.add(p[0].trim()+" "+p[1].trim());
        }
        Files.write(outDir.resolve("database.txt"),dbOut);
        Files.write(outDir.resolve("profits.txt"),profitOut);
        System.out.printf(Locale.ROOT,"Converted %d transactions and %d profits to %s%n",dbOut.size(),profitOut.size(),outDir.toAbsolutePath());
    }
}
