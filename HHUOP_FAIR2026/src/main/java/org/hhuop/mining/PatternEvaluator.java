package org.hhuop.mining;

import org.hhuop.model.*;
import java.util.*;

public final class PatternEvaluator {
    private PatternEvaluator() {}
    public static HUOPPattern evaluate(QuantitativeDatabase db, Itemset x) {
        List<Integer> tids=new ArrayList<>(); double sum=0;
        for(Transaction t:db.transactions()) if(x.isSubsetOf(t)) {
            double tu=t.transactionUtility(db.profits());
            double u=t.utility(x,db.profits());
            double local=tu<=0?0:u/tu;
            tids.add(t.tid()); sum += local;
        }
        int s=tids.size(); double uo=s==0?0:sum/s;
        return new HUOPPattern(x,s,sum,uo,tids);
    }
    public static boolean isHUOP(QuantitativeDatabase db, Itemset x, double alpha, double beta) {
        HUOPPattern p=evaluate(db,x); return p.support()>=db.minSupport(alpha)&&p.uo()+1e-12>=beta;
    }
}
