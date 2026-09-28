package org.hhuop.experiment;

import org.hhuop.io.DatabaseReader;import org.hhuop.mining.*;import org.hhuop.model.*;import org.hhuop.sanitize.*;import java.nio.file.*;import java.util.*;
public final class SelfTestMain{
 private static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[]argv)throws Exception{
  Path root=Path.of(argv.length>0?argv[0]:".");var db=DatabaseReader.read(root.resolve("datasets/example/database.txt"),root.resolve("datasets/example/profits.txt"));var miner=new VerticalHUOPMiner();var h=miner.mine(db,0.4,0.78);check(h.itemsets().contains(Itemset.of("a","c")),"{a,c} missing");check(h.itemsets().contains(Itemset.of("a","c","d")),"{a,c,d} missing");check(h.itemsets().size()==2,"Expected exactly 2 HUOPs, got "+h.itemsets());Set<Itemset>s=Set.of(Itemset.of("a","c"));for(AbstractHHUOPSanitizer alg:List.of(new HHUOPSMAU(miner,.001,1000,.05),new HHUOPSMIU(miner,.001,1000,.05),new HHUOPSMSE(miner,.001,1000,.05),new HHUOPDEL(miner,.001,1000,.05),new HHUOPHUI(miner,.001,1000,.05),new HHUOPGRED(miner,.001,1000,.05))){var rr=alg.sanitize(db,.4,.78,s,h,h.runtimeMs());check(rr.metrics().hidingFailures==0,alg.name()+" HF != 0");check(!rr.actions().isEmpty(),alg.name()+" made no edit");if(alg instanceof HHUOPDEL)for(var act:rr.actions())check(act.afterQuantity()==0,alg.name()+" kept a positive quantity");System.out.println(alg.name()+" PASS: "+rr.metrics().toCsv());}System.out.println("ALL TESTS PASSED");
 }
}
