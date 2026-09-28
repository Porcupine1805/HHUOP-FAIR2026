package org.hhuop.experiment;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.stream.*;
/** Combines per-configuration result CSVs into one chart-ready table. */
public final class AggregateResultsMain{
 public static void main(String[]v)throws Exception{Args a=new Args(v);Path dir=Path.of(a.get("inputDir","results/real")),out=Path.of(a.get("out","results/real/aggregate.csv"));List<String> rows=new ArrayList<>();String header=null;
  try(Stream<Path>s=Files.walk(dir)){for(Path p:s.filter(Files::isRegularFile).filter(x->x.toString().endsWith(".csv")).sorted().toList()){if(p.equals(out)||p.getFileName().toString().equals("aggregate.csv")||p.toString().contains("_actions"))continue;List<String>lines=Files.readAllLines(p);if(lines.size()<2||!lines.get(0).startsWith("dataset,"))continue;if(header==null)header=lines.get(0);else if(!header.equals(lines.get(0)))throw new IOException("CSV schema mismatch: "+p);rows.addAll(lines.subList(1,lines.size()));}}
  if(header==null)throw new IOException("No result CSV files under "+dir);Files.createDirectories(out.toAbsolutePath().getParent());List<String>all=new ArrayList<>();all.add(header);all.addAll(rows);Files.write(out,all);System.out.println("Aggregated "+rows.size()+" rows to "+out.toAbsolutePath());}
}
