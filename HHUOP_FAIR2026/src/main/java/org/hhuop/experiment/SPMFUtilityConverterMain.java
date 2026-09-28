package org.hhuop.experiment;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Converts SPMF item-list:TU:internal-utility format without changing item utilities. */
public final class SPMFUtilityConverterMain {
    public static void main(String[] argv)throws Exception{
        Args a=new Args(argv);Path input=Path.of(a.get("input","utility.txt"));Path out=Path.of(a.get("outDir","datasets/utility"));
        Files.createDirectories(out);Set<String> items=new TreeSet<>();long count=0;
        try(BufferedReader in=Files.newBufferedReader(input);BufferedWriter db=Files.newBufferedWriter(out.resolve("database.txt"))){
            for(String raw;(raw=in.readLine())!=null;){String line=raw.trim();if(line.isEmpty()||line.startsWith("#")||line.startsWith("%")||line.startsWith("@"))continue;
                String[] sections=line.split(":");if(sections.length!=3)throw new IOException("Bad SPMF utility record at transaction "+count+": "+line);
                String[] ids=sections[0].trim().split("\\s+"),utilities=sections[2].trim().split("\\s+");
                if(ids.length!=utilities.length)throw new IOException("Item/utility length mismatch at transaction "+count);
                long declared=Long.parseLong(sections[1].trim()),sum=0;db.write("T"+count);
                Set<String> seen=new HashSet<>();
                for(int i=0;i<ids.length;i++){String item="i"+ids[i];int utility=Integer.parseInt(utilities[i]);
                    if(utility<=0)throw new IOException("Internal utilities must be positive integers at transaction "+count);
                    if(!seen.add(item))throw new IOException("Duplicate item "+item+" at transaction "+count);
                    items.add(item);sum+=utility;db.write(" "+item+":"+utility);
                }
                if(sum!=declared)throw new IOException("Transaction utility mismatch at transaction "+count+": declared="+declared+" sum="+sum);
                db.newLine();count++;
            }
        }
        try(BufferedWriter profits=Files.newBufferedWriter(out.resolve("profits.txt"))){for(String item:items){profits.write(item+" 1");profits.newLine();}}
        Files.writeString(out.resolve("SOURCE.txt"),"Converted from "+input.toAbsolutePath()+System.lineSeparator()+
                "SPMF internal utility is represented as quantity; every unit profit is 1. Occupancy is preserved exactly."+System.lineSeparator());
        System.out.printf(Locale.ROOT,"Converted %,d transactions and %,d items to %s%n",count,items.size(),out.toAbsolutePath());
    }
}
