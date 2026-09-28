package org.hhuop.experiment;
import org.hhuop.io.DatabaseReader;import org.hhuop.mining.*;import org.hhuop.model.*;import java.nio.file.*;
public final class MineMain{
 public static void main(String[]argv)throws Exception{Args a=new Args(argv);var db=DatabaseReader.read(Path.of(a.get("db","datasets/example/database.txt")),Path.of(a.get("profits","datasets/example/profits.txt")));double alpha=a.getDouble("alpha",0.4),beta=a.getDouble("beta",0.78);var r=new VerticalHUOPMiner().mine(db,alpha,beta);if(!a.has("countOnly"))for(HUOPPattern p:r.patterns().values())System.out.printf(java.util.Locale.US,"%s sup=%d uo=%.8f tids=%s%n",p.itemset(),p.support(),p.uo(),p.tids());System.out.printf(java.util.Locale.US,"HUOPs=%d visited=%d time=%.3f ms%n",r.patterns().size(),r.visitedNodes(),r.runtimeMs());}
}
