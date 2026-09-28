package org.hhuop.experiment;

import org.hhuop.mining.*;
import org.hhuop.model.*;
import java.util.*;

/** Dependency-free randomized oracle/property tests for the exact miner and incremental evaluator. */
public final class OraclePropertyTestMain {
    private static final double TOL=1e-10;
    public static void main(String[] args){
        int cases=args.length==0?200:Integer.parseInt(args[0]);
        validationProperties();
        Random random=new Random(20260811L);
        for(int test=0;test<cases;test++) runCase(random,test);
        System.out.println("ORACLE/PROPERTY TESTS PASSED: "+cases+" randomized databases");
    }
    private static void validationProperties(){
        Map<String,Double> p=Map.of("a",1.0);Transaction t=new Transaction(1,Map.of("a",1));
        expectFailure(()->new QuantitativeDatabase(List.of(t,new Transaction(1,Map.of("a",2))),p),"duplicate TID");
        expectFailure(()->new QuantitativeDatabase(List.of(t),Map.of("a",0.0)),"zero profit");
        QuantitativeDatabase db=new QuantitativeDatabase(List.of(t),p);
        expectFailure(()->new VerticalHUOPMiner().mine(db,0,0.5),"invalid alpha");
        expectFailure(()->new VerticalHUOPMiner().mine(db,0.5,1.1),"invalid beta");
    }
    private static void expectFailure(Runnable action,String name){
        try{action.run();throw new AssertionError("Expected validation failure: "+name);}catch(IllegalArgumentException expected){/* pass */}
    }
    private static void runCase(Random r,int test){
        int itemCount=1+r.nextInt(7),txCount=1+r.nextInt(9);
        Map<String,Double> profits=new LinkedHashMap<>();
        for(int i=0;i<itemCount;i++)profits.put("i"+i,(double)(1+r.nextInt(9)));
        List<Transaction> transactions=new ArrayList<>();
        for(int tid=0;tid<txCount;tid++){
            Map<String,Integer> q=new LinkedHashMap<>();
            for(String item:profits.keySet())if(r.nextDouble()<0.5)q.put(item,1+r.nextInt(5));
            if(q.isEmpty())q.put("i"+r.nextInt(itemCount),1+r.nextInt(5));
            transactions.add(new Transaction(tid*2+1,q)); // deliberately non-contiguous TIDs
        }
        QuantitativeDatabase db=new QuantitativeDatabase(transactions,profits);
        double alpha=(1+r.nextInt(txCount))/(double)txCount,beta=r.nextDouble();
        Map<Itemset,HUOPPattern> oracle=bruteForce(db,alpha,beta);
        HUOPMiningResult actual=new VerticalHUOPMiner().mine(db,alpha,beta);
        check(oracle.keySet().equals(actual.itemsets()),"itemsets",test,oracle.keySet(),actual.itemsets());
        for(Itemset x:oracle.keySet()){
            HUOPPattern a=oracle.get(x),b=actual.get(x);
            check(a.support()==b.support()&&Math.abs(a.uo()-b.uo())<TOL,"stats "+x,test,a,b);
        }
        List<Itemset> all=allItemsets(new ArrayList<>(profits.keySet()));
        IncrementalPatternIndex index=new IncrementalPatternIndex(db,all);
        Transaction t=transactions.get(r.nextInt(transactions.size()));String item=t.items().iterator().next();int k=1+r.nextInt(t.quantity(item));
        QuantitativeDatabase changed=db.copy();changed.reduce(t.tid(),item,k);
        for(Itemset x:all){
            HUOPPattern predicted=index.evaluateAfter(x,t,item,k),expected=PatternEvaluator.evaluate(changed,x);
            check(predicted.support()==expected.support()&&Math.abs(predicted.sumUO()-expected.sumUO())<TOL,"incremental "+x,test,expected,predicted);
        }
        index.applyReduction(t,item,k);db.reduce(t.tid(),item,k);
        for(Itemset x:all){HUOPPattern a=index.evaluate(x),b=PatternEvaluator.evaluate(db,x);
            check(a.support()==b.support()&&Math.abs(a.sumUO()-b.sumUO())<TOL,"applied delta "+x,test,b,a);}
    }
    private static Map<Itemset,HUOPPattern> bruteForce(QuantitativeDatabase db,double alpha,double beta){
        Map<Itemset,HUOPPattern> out=new LinkedHashMap<>();
        for(Itemset x:allItemsets(new ArrayList<>(db.allItems()))){HUOPPattern p=PatternEvaluator.evaluate(db,x);
            if(p.support()>=db.minSupport(alpha)&&p.uo()+1e-12>=beta)out.put(x,p);}
        return out;
    }
    private static List<Itemset> allItemsets(List<String> items){
        List<Itemset> out=new ArrayList<>();int limit=1<<items.size();
        for(int mask=1;mask<limit;mask++){List<String>x=new ArrayList<>();for(int i=0;i<items.size();i++)if((mask&(1<<i))!=0)x.add(items.get(i));out.add(new Itemset(x));}
        return out;
    }
    private static void check(boolean ok,String what,int test,Object expected,Object actual){
        if(!ok)throw new AssertionError("Case "+test+" "+what+" expected="+expected+" actual="+actual);
    }
}
