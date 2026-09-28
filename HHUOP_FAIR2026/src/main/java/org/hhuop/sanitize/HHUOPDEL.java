package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.HUOPMiner;
import java.util.*;

/** Deletion-only baseline. Same victim and maximum-utility item as SMAU, but the item is always removed. */
public final class HHUOPDEL extends AbstractHHUOPSanitizer {
    public HHUOPDEL(HUOPMiner miner,double epsilon,int maxActions,double borderEta){super(miner,epsilon,maxActions,borderEta);}
    @Override public String name(){return "HHUOP-DEL";}
    @Override protected Candidate chooseAction(QuantitativeDatabase db,Itemset target,Transaction t,
            Set<Itemset>sensitive,Set<Itemset>nonSensitive,Set<Itemset>border,org.hhuop.mining.IncrementalPatternIndex index,
            org.hhuop.model.HUOPPattern targetStats,double alpha,double beta){
        String best=maximumUtilityItem(db,target,t);
        return best==null?null:deletionCandidate(db,target,t,best);
    }
}
