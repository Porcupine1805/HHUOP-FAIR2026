package org.hhuop.mining;

import org.hhuop.model.*;
import java.util.*;

/** Exact incremental statistics for a fixed collection of monitored patterns. */
public final class IncrementalPatternIndex {
    private static final double TOL=1e-12;
    private static final class State {
        int support;
        double sumUO;
        final LinkedHashSet<Integer> tids=new LinkedHashSet<>();
    }
    private final QuantitativeDatabase db;
    private final Map<Itemset,State> states=new HashMap<>();
    private final Map<Integer,List<Itemset>> patternsByTid=new HashMap<>();

    public IncrementalPatternIndex(QuantitativeDatabase db,Collection<Itemset> patterns){
        this.db=db;
        for(Itemset x:new LinkedHashSet<>(patterns)){
            State s=new State();
            for(Transaction t:db.transactions()) if(x.isSubsetOf(t)){
                double local=localOccupancy(t,x,0.0,null);
                s.support++; s.sumUO+=local; s.tids.add(t.tid());
                patternsByTid.computeIfAbsent(t.tid(),k->new ArrayList<>()).add(x);
            }
            states.put(x,s);
        }
    }
    /** Builds the index from exact mining results without rescanning the database per pattern. */
    public IncrementalPatternIndex(QuantitativeDatabase db,Map<Itemset,HUOPPattern> patterns){
        this.db=db;
        for(var entry:patterns.entrySet()){
            Itemset x=entry.getKey();HUOPPattern p=entry.getValue();State s=new State();
            s.support=p.support();s.sumUO=p.sumUO();s.tids.addAll(p.tids());states.put(x,s);
            for(int tid:p.tids())patternsByTid.computeIfAbsent(tid,k->new ArrayList<>()).add(x);
        }
    }

    public HUOPPattern evaluate(Itemset x){
        State s=require(x);
        return pattern(x,s.support,s.sumUO,s.tids);
    }
    public boolean isHUOP(Itemset x,double alpha,double beta){
        State s=require(x); return isHUOP(s.support,s.sumUO,db.minSupport(alpha),beta);
    }

    /** Predict exact statistics after reducing one cell, without mutating the database. */
    public HUOPPattern evaluateAfter(Itemset x,Transaction t,String item,int k){
        State s=require(x);
        if(!s.tids.contains(t.tid())) return evaluate(x);
        double loss=k*db.profits().get(item);
        double oldLocal=localOccupancy(t,x,0.0,null);
        boolean losesSupport=x.contains(item)&&k==t.quantity(item);
        int support=s.support-(losesSupport?1:0);
        double sum=s.sumUO-oldLocal+(losesSupport?0:localOccupancy(t,x,loss,item));
        if(!losesSupport)return pattern(x,support,sum,s.tids);
        List<Integer> tids=new ArrayList<>(s.tids);tids.remove(Integer.valueOf(t.tid()));
        return pattern(x,support,sum,tids);
    }
    /** Allocation-free HUOP prediction used in the candidate hot path. */
    public boolean isHUOPAfter(Itemset x,Transaction t,String item,int k,int minsup,double beta){
        State s=require(x);if(!s.tids.contains(t.tid()))return isHUOP(s.support,s.sumUO,minsup,beta);
        double loss=k*db.profits().get(item),oldLocal=localOccupancy(t,x,0.0,null);
        boolean losesSupport=x.contains(item)&&k==t.quantity(item);
        int support=s.support-(losesSupport?1:0);
        double sum=s.sumUO-oldLocal+(losesSupport?0:localOccupancy(t,x,loss,item));
        return isHUOP(support,sum,minsup,beta);
    }

    /** Update all monitored patterns affected by this reduction. Call before mutating the transaction. */
    public void applyReduction(Transaction t,String item,int k){
        List<Itemset> affected=patternsByTid.getOrDefault(t.tid(),List.of());
        double loss=k*db.profits().get(item);
        for(Itemset x:affected){
            State s=states.get(x);
            if(!s.tids.contains(t.tid())) continue;
            double oldLocal=localOccupancy(t,x,0.0,null);
            boolean losesSupport=x.contains(item)&&k==t.quantity(item);
            s.sumUO-=oldLocal;
            if(losesSupport){s.support--;s.tids.remove(t.tid());}
            else s.sumUO+=localOccupancy(t,x,loss,item);
        }
    }

    public List<Itemset> supportedPatterns(int tid){return patternsByTid.getOrDefault(tid,List.of());}
    public boolean supports(Itemset x,int tid){return require(x).tids.contains(tid);}

    private double localOccupancy(Transaction t,Itemset x,double loss,String reducedItem){
        double tu=t.transactionUtility(db.profits())-loss;
        if(tu<=0) return 0;
        double u=t.utility(x,db.profits());
        if(reducedItem!=null&&x.contains(reducedItem)) u-=loss;
        return u/tu;
    }
    private State require(Itemset x){
        State s=states.get(x); if(s==null) throw new IllegalArgumentException("Pattern is not monitored: "+x); return s;
    }
    private static HUOPPattern pattern(Itemset x,int support,double sum,Collection<Integer> tids){
        return new HUOPPattern(x,support,sum,support==0?0:sum/support,new ArrayList<>(tids));
    }
    public static boolean isHUOP(HUOPPattern p,int minsup,double beta){return isHUOP(p.support(),p.sumUO(),minsup,beta);}
    private static boolean isHUOP(int support,double sum,int minsup,double beta){
        return support>=minsup&&support>0&&sum/support+TOL>=beta;
    }
}
