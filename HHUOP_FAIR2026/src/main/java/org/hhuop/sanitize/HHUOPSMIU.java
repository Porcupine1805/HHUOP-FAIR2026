package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.HUOPMiner;
import java.util.*;

/** Selecting Minimum Utility item first, adapted to HUOP hiding. */
public final class HHUOPSMIU extends AbstractHHUOPSanitizer {
    public HHUOPSMIU(HUOPMiner miner,double epsilon,int maxActions,double borderEta){super(miner,epsilon,maxActions,borderEta);}
    @Override public String name(){return "HHUOP-SMIU";}
    @Override protected Candidate chooseAction(QuantitativeDatabase db,Itemset target,Transaction t,
            Set<Itemset>sensitive,Set<Itemset>nonSensitive,Set<Itemset>border,org.hhuop.mining.IncrementalPatternIndex index,
            org.hhuop.model.HUOPPattern targetStats,double alpha,double beta){
        String best=null; double bu=Double.POSITIVE_INFINITY;
        for(String i:target.items()){
            double u=t.utility(i,db.profits());
            if(u<bu-1e-12 || (Math.abs(u-bu)<1e-12 && (best==null||i.compareTo(best)<0))){bu=u;best=i;}
        }
        return best==null?null:exactCandidate(db,target,t,best,targetStats,alpha,beta);
    }
}
