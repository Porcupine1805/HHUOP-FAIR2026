package org.hhuop.experiment;
import org.hhuop.io.DatabaseReader;import org.hhuop.model.*;import java.nio.file.*;import java.util.*;
public final class DatabaseStatsMain{
 public static void main(String[]v)throws Exception{Args a=new Args(v);QuantitativeDatabase db=DatabaseReader.read(Path.of(a.get("db","database.txt")),Path.of(a.get("profits","profits.txt")));long cells=db.nonzeroCells();
  System.out.printf(Locale.ROOT,"transactions=%d items=%d cells=%d avgLength=%.4f totalQuantity=%d totalUtility=%.4f%n",db.size(),db.allItems().size(),cells,cells/(double)db.size(),db.totalQuantity(),db.totalUtility());}
}
