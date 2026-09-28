package org.hhuop.mining;

import org.hhuop.model.*;
import java.util.*;

/** Exact, allocation-conscious vertical utility-occupancy miner. */
public final class VerticalHUOPMiner implements HUOPMiner {
    @Override public HUOPMiningResult mine(QuantitativeDatabase db,double alpha,double beta){
        validate(db,alpha,beta);
        return new Search(db,db.minSupport(alpha),beta).run();
    }

    private static final class Vertical {
        final int[] tids;
        final double[] utility,remaining,tu;
        int size;
        Vertical(int capacity){tids=new int[capacity];utility=new double[capacity];remaining=new double[capacity];tu=new double[capacity];}
    }
    private static final class UOList {
        final int[] tids;
        final double[] iutil,rutil,tu;
        final int size;
        UOList(int[] tids,double[] iutil,double[] rutil,double[] tu,int size){this.tids=tids;this.iutil=iutil;this.rutil=rutil;this.tu=tu;this.size=size;}
        static UOList root(Vertical v){return new UOList(v.tids,v.utility,v.remaining,v.tu,v.size);}
    }
    private static final class Search {
        final QuantitativeDatabase db;
        final int minsup;
        final double beta;
        final LinkedHashMap<Itemset,HUOPPattern> out=new LinkedHashMap<>();
        List<String> order;
        Map<String,Integer> pos;
        Vertical[] verticals;
        long visited;
        long start;

        Search(QuantitativeDatabase db,int minsup,double beta){this.db=db;this.minsup=minsup;this.beta=beta;}
        HUOPMiningResult run(){
            start=System.nanoTime(); preprocess();
            for(int i=0;i<order.size();i++) if(verticals[i].size>=minsup)
                expand(new ArrayList<>(List.of(order.get(i))),i,UOList.root(verticals[i]));
            return new HUOPMiningResult(out,visited,System.nanoTime()-start);
        }
        void preprocess(){
            Map<String,Integer> support=new HashMap<>();
            for(Transaction t:db.transactions()) for(String item:t.items()) support.merge(item,1,Integer::sum);
            order=new ArrayList<>(db.allItems());
            order.sort(Comparator.comparingInt((String x)->support.getOrDefault(x,0)).thenComparing(x->x));
            pos=new HashMap<>(Math.max(16,order.size()*2));
            verticals=new Vertical[order.size()];
            for(int i=0;i<order.size();i++){pos.put(order.get(i),i);verticals[i]=new Vertical(support.getOrDefault(order.get(i),0));}

            for(Transaction t:db.transactions()){
                int n=t.itemCount(); int[] positions=new int[n]; int z=0;
                for(String item:t.items()) positions[z++]=pos.get(item);
                Arrays.sort(positions);
                double suffix=0,tu=t.transactionUtility(db.profits());
                for(int a=n-1;a>=0;a--){
                    int p=positions[a]; Vertical v=verticals[p]; int q=v.size++;
                    String item=order.get(p); double utility=t.utility(item,db.profits());
                    v.tids[q]=t.tid();v.utility[q]=utility;v.remaining[q]=suffix;v.tu[q]=tu;
                    suffix+=utility;
                }
            }
        }
        void expand(List<String> prefix,int last,UOList list){
            visited++; if(list.size<minsup)return;
            double sum=0; for(int i=0;i<list.size;i++)sum+=safeDiv(list.iutil[i],list.tu[i]);
            Itemset x=new Itemset(prefix); double uo=sum/list.size;
            if(uo+1e-12>=beta){
                List<Integer> tids=new ArrayList<>(list.size);for(int i=0;i<list.size;i++)tids.add(list.tids[i]);
                out.put(x,new HUOPPattern(x,list.size,sum,uo,tids));
            }
            if(last+1>=order.size()||upperBound(list)+1e-12<beta)return;
            for(int extension=last+1;extension<order.size();extension++){
                UOList child=join(list,verticals[extension]);
                if(child.size<minsup)continue;
                prefix.add(order.get(extension));expand(prefix,extension,child);prefix.remove(prefix.size()-1);
            }
        }
        UOList join(UOList parent,Vertical extension){
            int capacity=Math.min(parent.size,extension.size);
            int[] tids=new int[capacity];double[] iu=new double[capacity],ru=new double[capacity],tu=new double[capacity];
            int a=0,b=0,n=0;
            while(a<parent.size&&b<extension.size){
                int pt=parent.tids[a],et=extension.tids[b];
                if(pt<et)a++; else if(pt>et)b++; else{
                    tids[n]=pt;iu[n]=parent.iutil[a]+extension.utility[b];ru[n]=extension.remaining[b];tu[n]=parent.tu[a];
                    a++;b++;n++;
                }
            }
            return new UOList(tids,iu,ru,tu,n);
        }
        double upperBound(UOList list){
            // A min-heap keeps only the minsup largest bounds instead of sorting the whole list.
            PriorityQueue<Double> top=new PriorityQueue<>(minsup);
            for(int i=0;i<list.size;i++){
                double value=safeDiv(list.iutil[i]+list.rutil[i],list.tu[i]);
                if(top.size()<minsup)top.add(value); else if(value>top.peek()){top.poll();top.add(value);}
            }
            double sum=0;for(double value:top)sum+=value;return top.isEmpty()?0:sum/top.size();
        }
    }
    private static double safeDiv(double a,double b){return b<=0?0:a/b;}
    private static void validate(QuantitativeDatabase db,double alpha,double beta){
        if(db==null||db.size()==0)throw new IllegalArgumentException("Database must not be empty");
        if(!Double.isFinite(alpha)||alpha<=0||alpha>1)throw new IllegalArgumentException("alpha must be in (0,1]");
        if(!Double.isFinite(beta)||beta<0||beta>1)throw new IllegalArgumentException("beta must be in [0,1]");
    }
}
