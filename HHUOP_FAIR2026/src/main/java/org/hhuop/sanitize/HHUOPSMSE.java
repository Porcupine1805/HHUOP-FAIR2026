package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.*;
import java.util.*;

/** Selecting Minimum Side Effects item first with HUOP-specific missing/artificial-risk estimates. */
public final class HHUOPSMSE extends AbstractHHUOPSanitizer {
    private final double wM,wA,wD,wS;
    public HHUOPSMSE(HUOPMiner miner,double epsilon,int maxActions,double borderEta){this(miner,epsilon,maxActions,borderEta,1.0,1.0,0.1,0.5);}
    public HHUOPSMSE(HUOPMiner miner,double epsilon,int maxActions,double borderEta,double wM,double wA,double wD,double wS){
        super(miner,epsilon,maxActions,borderEta);this.wM=wM;this.wA=wA;this.wD=wD;this.wS=wS;
    }
    @Override public String name(){return "HHUOP-SMSE";}

    @Override protected Candidate chooseAction(QuantitativeDatabase db,Itemset target,Transaction t,
            Set<Itemset>sensitive,Set<Itemset>nonSensitive,Set<Itemset>border,IncrementalPatternIndex index,
            HUOPPattern targetStats,double alpha,double beta){
        Candidate best=null; double bestUtility=Double.NEGATIVE_INFINITY;
        int minsup=db.minSupport(alpha);
        List<Itemset> affected=index.supportedPatterns(t.tid());
        for(String item:target.items()){
            Candidate raw=exactCandidate(db,target,t,item,targetStats,alpha,beta); if(raw==null) continue;
            int beforeQ=t.quantity(item); int afterQ=beforeQ-raw.k();
            double loss=raw.k()*db.profits().get(item);
            int shc=0,mr=0,ar=0;
            for(Itemset x:affected){
                if(!index.supports(x,t.tid()))continue;
                boolean before=index.isHUOP(x,alpha,beta);
                if(sensitive.contains(x)&&x.contains(item)&&before) shc++;
                if(!nonSensitive.contains(x)&&!border.contains(x)) continue;
                boolean after=index.isHUOPAfter(x,t,item,raw.k(),minsup,beta);
                if(nonSensitive.contains(x)&&before&&!after) mr++;
                else if(border.contains(x)&&!before&&after) ar++;
            }

            double total=db.totalUtility();
            double dist=total==0?0:loss/total;
            double score=(wM*mr+wA*ar+wD*dist)/(1.0+wS*shc);
            Candidate c=new Candidate(t.tid(),item,raw.k(),raw.deletion(),mr,ar,dist,shc,score);
            double util=t.utility(item,db.profits());
            if(best==null || score<best.score()-1e-12 ||
                    (Math.abs(score-best.score())<1e-12 && util>bestUtility+1e-12) ||
                    (Math.abs(score-best.score())<1e-12 && Math.abs(util-bestUtility)<1e-12 && item.compareTo(best.item())<0)){
                best=c;bestUtility=util;
            }
        }
        return best;
    }
}
