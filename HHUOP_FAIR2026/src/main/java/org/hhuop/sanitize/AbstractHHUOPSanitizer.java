package org.hhuop.sanitize;

import org.hhuop.model.*;
import org.hhuop.mining.*;
import org.hhuop.util.MemorySampler;
import java.util.*;

/** Common verify-and-repair framework for HHUOP-SMAU/SMIU/SMSE. */
public abstract class AbstractHHUOPSanitizer {
    protected record Candidate(int tid, String item, int k, boolean deletion,
                               int missing, int artificial, double dist, int shc, double score) {}

    protected final HUOPMiner miner;
    protected final double epsilon;
    protected final int maxActions;
    protected final double borderEta;

    protected AbstractHHUOPSanitizer(HUOPMiner miner, double epsilon, int maxActions, double borderEta) {
        this.miner=miner; this.epsilon=epsilon; this.maxActions=maxActions; this.borderEta=borderEta;
    }
    public abstract String name();
    protected abstract Candidate chooseAction(QuantitativeDatabase db, Itemset target, Transaction t,
                                               Set<Itemset> sensitive, Set<Itemset> nonSensitive,
                                               Set<Itemset> border, IncrementalPatternIndex index,
                                               HUOPPattern targetStats,double alpha, double beta);

    public SanitizationResult sanitize(QuantitativeDatabase original, double alpha, double beta,
                                       Set<Itemset> sensitive, HUOPMiningResult originalMining,
                                       double initialMiningMs) {
        return sanitize(original,alpha,beta,sensitive,originalMining,initialMiningMs,0);
    }
    public SanitizationResult sanitize(QuantitativeDatabase original, double alpha, double beta,
                                       Set<Itemset> sensitive, HUOPMiningResult originalMining,
                                       double initialMiningMs,double initialMiningPeakMB) {
        validateParameters(original,alpha,beta,sensitive);
        long wallStart=System.nanoTime(),setupStart=wallStart;
        try(MemorySampler ms=new MemorySampler()) {
            QuantitativeDatabase db=original.copy();
            Set<Itemset> originalH=new LinkedHashSet<>(originalMining.itemsets());
            Set<Itemset> nonSensitive=new LinkedHashSet<>(originalH); nonSensitive.removeAll(sensitive);
            Map<Itemset,HUOPPattern> borderPatterns=mineBorder(original,alpha,beta,originalH);
            Set<Itemset> border=borderPatterns.keySet();
            Map<Itemset,HUOPPattern> monitored=new LinkedHashMap<>(originalMining.patterns());monitored.putAll(borderPatterns);
            IncrementalPatternIndex index=new IncrementalPatternIndex(db,monitored);
            Map<Integer,Integer> nonSensitiveCoverage=new HashMap<>();
            for(Itemset x:nonSensitive)for(int tid:originalMining.get(x).tids())nonSensitiveCoverage.merge(tid,1,Integer::sum);
            List<SanitizationAction> log=new ArrayList<>();
            long setupNanos=System.nanoTime()-setupStart,actionNanos=0,verifyNanos=0;
            HUOPMiningResult finalMine=null;
            boolean progress=true;
            while(progress && log.size()<maxActions) {
                progress=false;
                List<Itemset> orderedS=new ArrayList<>(sensitive);
                orderedS.sort(Comparator.comparingInt(Itemset::size).reversed().thenComparing(Itemset::key));
                for(Itemset x:orderedS) {
                    while(index.isHUOP(x,alpha,beta) && log.size()<maxActions) {
                        long s0=System.nanoTime();
                        HUOPPattern stats=index.evaluate(x);
                        Transaction victim=selectVictimTransaction(db,stats.tids(),nonSensitiveCoverage);
                        if(victim==null) break;
                        Candidate c=chooseAction(db,x,victim,sensitive,nonSensitive,border,index,stats,alpha,beta);
                        if(c==null || c.k<=0) break;
                        int before=victim.quantity(c.item);
                        double unit=db.profits().get(c.item);
                        if(c.k==before){
                            int removed=0;for(Itemset y:index.supportedPatterns(victim.tid()))
                                if(nonSensitive.contains(y)&&y.contains(c.item)&&index.supports(y,victim.tid()))removed++;
                            if(removed>0)nonSensitiveCoverage.merge(victim.tid(),-removed,Integer::sum);
                        }
                        index.applyReduction(victim,c.item,c.k);
                        db.reduce(victim.tid(),c.item,c.k);
                        int after=victim.quantity(c.item);
                        log.add(new SanitizationAction(log.size()+1,name(),x,victim.tid(),c.item,before,after,c.k*unit,
                                c.missing,c.artificial,c.score));
                        actionNanos += System.nanoTime()-s0;
                        progress=true;
                    }
                }
                long v0=System.nanoTime();
                finalMine=miner.mine(db,alpha,beta);
                verifyNanos += System.nanoTime()-v0;
                boolean any=false; for(Itemset x:sensitive) if(finalMine.itemsets().contains(x)){any=true;break;}
                if(!any) break;
            }
            if(finalMine==null){ long v0=System.nanoTime(); finalMine=miner.mine(db,alpha,beta); verifyNanos+=System.nanoTime()-v0; }
            List<Itemset> remaining=new ArrayList<>();
            for(Itemset x:sensitive) if(finalMine.itemsets().contains(x)) remaining.add(x);
            if(!remaining.isEmpty()) {
                throw new IllegalStateException("Sanitization stopped before HF=0 (maxActions/no progress). Remaining sensitive HUOPs: "+remaining);
            }
            SanitizationMetrics metrics=computeMetrics(original,db,originalH,sensitive,nonSensitive,finalMine,log,
                    initialMiningMs,setupNanos/1e6,actionNanos/1e6,verifyNanos/1e6,
                    0,Math.max(initialMiningPeakMB,ms.peakMB()));
            metrics.endToEndSanitizationMs=(System.nanoTime()-wallStart)/1e6;
            metrics.totalMs=initialMiningMs+metrics.endToEndSanitizationMs;
            return new SanitizationResult(db,finalMine,metrics,log);
        }
    }

    protected Transaction selectVictimTransaction(QuantitativeDatabase db,List<Integer> tids,Map<Integer,Integer> coverage) {
        Transaction best=null; int bestN=Integer.MAX_VALUE;
        for(int tid:tids){
            Transaction t=db.transactionByTid(tid); int n=coverage.getOrDefault(tid,0);
            if(best==null || n<bestN || (n==bestN && t.tid()<best.tid())){best=t;bestN=n;}
        }
        return best;
    }

    protected Candidate exactCandidate(QuantitativeDatabase db, Itemset x, Transaction t, String item,
                                       HUOPPattern p,double alpha, double beta) {
        int s=p.support(); if(s==0 || !x.isSubsetOf(t)) return null;
        double B=t.transactionUtility(db.profits());
        double A=t.utility(x,db.profits());
        if(B<=0) return null;
        double r=A/B;
        double lambda=Math.max(0,beta-epsilon);
        double tau=s*lambda-(p.sumUO()-r);
        int q=t.quantity(item); if(q<=0) return null;
        double pi=db.profits().get(item);
        if(tau>=0 && tau<r-1e-15 && tau<1-1e-15 && A<B-1e-12) {
            double delta=Math.max(0,(A-tau*B)/(1-tau));
            int k=(int)Math.ceil(delta/pi-1e-12); if(k<1) k=1;
            if(k<=q-1) return new Candidate(t.tid(),item,k,false,0,0,k*pi,0,Double.NaN);
        }
        return new Candidate(t.tid(),item,q,true,0,0,q*pi,0,Double.NaN);
    }

    /** HHUIF step: (A-Δ)/B ≤ τ, with transaction utility held fixed. Delete when that step cannot keep support. */
    protected Candidate huiCandidate(QuantitativeDatabase db, Itemset x, Transaction t, String item,
                                     HUOPPattern p, double beta) {
        int s=p.support(); if(s==0 || !x.isSubsetOf(t)) return null;
        double B=t.transactionUtility(db.profits());
        double A=t.utility(x,db.profits());
        if(B<=0) return null;
        double r=A/B;
        double lambda=Math.max(0,beta-epsilon);
        double tau=s*lambda-(p.sumUO()-r);
        int q=t.quantity(item); if(q<=0) return null;
        double pi=db.profits().get(item);
        if(tau>=0 && tau<r-1e-15 && tau<1-1e-15 && A<B-1e-12) {
            double delta=Math.max(0,A-tau*B);
            int k=(int)Math.ceil(delta/pi-1e-12); if(k<1) k=1;
            if(k<=q-1) return new Candidate(t.tid(),item,k,false,0,0,k*pi,0,Double.NaN);
        }
        return new Candidate(t.tid(),item,q,true,0,0,q*pi,0,Double.NaN);
    }

    /** Remove the chosen item from the victim transaction. */
    protected Candidate deletionCandidate(QuantitativeDatabase db, Itemset x, Transaction t, String item) {
        int q=t.quantity(item);
        if(q<=0 || !x.contains(item) || !x.isSubsetOf(t)) return null;
        double pi=db.profits().get(item);
        return new Candidate(t.tid(),item,q,true,0,0,q*pi,0,Double.NaN);
    }

    protected static String maximumUtilityItem(QuantitativeDatabase db, Itemset target, Transaction t) {
        String best=null; double bu=Double.NEGATIVE_INFINITY;
        for(String i:target.items()){
            if(t.quantity(i)<=0) continue;
            double u=t.utility(i,db.profits());
            if(best==null || u>bu+1e-12 || (Math.abs(u-bu)<1e-12 && i.compareTo(best)<0)){bu=u;best=i;}
        }
        return best;
    }

    protected Map<Itemset,HUOPPattern> mineBorder(QuantitativeDatabase db,double alpha,double beta,Set<Itemset> originalH){
        if(borderEta<=0 || beta<=0) return Map.of();
        double low=Math.max(0,beta-borderEta);
        HUOPMiningResult near=miner.mine(db,alpha,low);
        Map<Itemset,HUOPPattern>b=new LinkedHashMap<>();
        for(var e:near.patterns().entrySet()) if(!originalH.contains(e.getKey()) && e.getValue().uo()<beta) b.put(e.getKey(),e.getValue());
        return b;
    }

    protected static boolean isHUOP(QuantitativeDatabase db, Itemset x,double alpha,double beta){
        return PatternEvaluator.isHUOP(db,x,alpha,beta);
    }

    private SanitizationMetrics computeMetrics(QuantitativeDatabase orig, QuantitativeDatabase fin,
            Set<Itemset> originalH, Set<Itemset> sensitive, Set<Itemset> nonSensitive,
            HUOPMiningResult finalMine, List<SanitizationAction> log,
            double initialMiningMs,double setupMs,double sanitizationMs,double verificationMs,
            double endToEndSanitizationMs,double peakMB){
        SanitizationMetrics m=new SanitizationMetrics(); m.algorithm=name();
        Set<Itemset> H2=finalMine.itemsets();
        m.originalHUOPs=originalH.size();m.sensitiveCount=sensitive.size();m.finalHUOPs=H2.size();m.actions=log.size();
        for(Itemset x:sensitive) if(H2.contains(x)) m.hidingFailures++;
        for(Itemset x:nonSensitive) if(!H2.contains(x)) m.missingCount++;
        for(Itemset x:H2) if(!originalH.contains(x)) m.artificialCount++;
        m.hf=ratio(m.hidingFailures,sensitive.size());
        m.mc=ratio(m.missingCount,nonSensitive.size());
        m.ac=ratio(m.artificialCount,H2.size());
        double u0=orig.totalUtility(),u1=fin.totalUtility(); m.dus=u0==0?1:u1/u0; m.utilityLoss=1-m.dus;
        long changedCells=0,changedTx=0;
        for(Transaction t0:orig.transactions()){
            Transaction t1=fin.transactionByTid(t0.tid()); boolean changed=false;
            Set<String>items=new HashSet<>(t0.items());items.addAll(t1.items());
            for(String i:items) if(t0.quantity(i)!=t1.quantity(i)){changedCells++;changed=true;}
            if(changed)changedTx++;
        }
        m.od=ratio(changedCells,orig.nonzeroCells());m.tdr=ratio(changedTx,orig.size());
        m.qdr=orig.totalQuantity()==0?0:(orig.totalQuantity()-fin.totalQuantity())/(double)orig.totalQuantity();
        m.initialMiningMs=initialMiningMs;m.setupMs=setupMs;m.sanitizationMs=sanitizationMs;m.verificationMs=verificationMs;
        m.endToEndSanitizationMs=endToEndSanitizationMs;m.totalMs=initialMiningMs+endToEndSanitizationMs;m.peakMemoryMB=peakMB;
        return m;
    }
    private static double ratio(long a,long b){return b==0?0:a/(double)b;}
    private void validateParameters(QuantitativeDatabase db,double alpha,double beta,Set<Itemset>sensitive){
        if(db.size()==0)throw new IllegalArgumentException("Database must not be empty");
        if(!Double.isFinite(alpha)||alpha<=0||alpha>1)throw new IllegalArgumentException("alpha must be in (0,1]");
        if(!Double.isFinite(beta)||beta<0||beta>1)throw new IllegalArgumentException("beta must be in [0,1]");
        if(!Double.isFinite(epsilon)||epsilon<0)throw new IllegalArgumentException("epsilon must be finite and >= 0");
        if(!Double.isFinite(borderEta)||borderEta<0)throw new IllegalArgumentException("borderEta must be finite and >= 0");
        if(maxActions<=0)throw new IllegalArgumentException("maxActions must be > 0");
        if(sensitive==null||sensitive.isEmpty())throw new IllegalArgumentException("Sensitive set must not be empty");
    }
}
