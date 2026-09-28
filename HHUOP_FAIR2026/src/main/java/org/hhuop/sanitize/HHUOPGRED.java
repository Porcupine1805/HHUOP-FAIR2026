package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.*;
import java.util.*;

/** Greedy occupancy baseline. Reduce one unit of the item that most lowers occupancy of the sensitive HUOP. */
public final class HHUOPGRED extends AbstractHHUOPSanitizer {
    public HHUOPGRED(HUOPMiner miner,double epsilon,int maxActions,double borderEta){super(miner,epsilon,maxActions,borderEta);}
    @Override public String name(){return "HHUOP-GRED";}
    @Override protected Candidate chooseAction(QuantitativeDatabase db,Itemset target,Transaction t,
            Set<Itemset>sensitive,Set<Itemset>nonSensitive,Set<Itemset>border,IncrementalPatternIndex index,
            HUOPPattern targetStats,double alpha,double beta){
        Candidate best=null; double bestDrop=Double.NEGATIVE_INFINITY;
        double current=targetStats.uo();
        for(String item:target.items()){
            if(t.quantity(item)<=0) continue;
            HUOPPattern after=index.evaluateAfter(target,t,item,1);
            double drop=current-after.uo();
            double loss=db.profits().get(item);
            boolean deletion=t.quantity(item)==1;
            Candidate c=new Candidate(t.tid(),item,1,deletion,0,0,loss,0,drop);
            if(best==null || drop>bestDrop+1e-12
                    || (Math.abs(drop-bestDrop)<1e-12 && loss<best.dist()-1e-12)
                    || (Math.abs(drop-bestDrop)<1e-12 && Math.abs(loss-best.dist())<1e-12 && item.compareTo(best.item())<0)){
                best=c; bestDrop=drop;
            }
        }
        if(best==null) return null;
        if(bestDrop<=1e-12){
            String item=maximumUtilityItem(db,target,t);
            return item==null?null:deletionCandidate(db,target,t,item);
        }
        return best;
    }
}
