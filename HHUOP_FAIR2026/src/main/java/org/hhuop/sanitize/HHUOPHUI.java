package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.HUOPMiner;
import java.util.*;

/** Traditional HUI baseline. HHUIF freezes the denominator; the outer loop then re-mines HUOPs. */
public final class HHUOPHUI extends AbstractHHUOPSanitizer {
    public HHUOPHUI(HUOPMiner miner,double epsilon,int maxActions,double borderEta){super(miner,epsilon,maxActions,borderEta);}
    @Override public String name(){return "HHUOP-HUI";}
    @Override protected Candidate chooseAction(QuantitativeDatabase db,Itemset target,Transaction t,
            Set<Itemset>sensitive,Set<Itemset>nonSensitive,Set<Itemset>border,org.hhuop.mining.IncrementalPatternIndex index,
            org.hhuop.model.HUOPPattern targetStats,double alpha,double beta){
        String best=maximumUtilityItem(db,target,t);
        return best==null?null:huiCandidate(db,target,t,best,targetStats,beta);
    }
}
