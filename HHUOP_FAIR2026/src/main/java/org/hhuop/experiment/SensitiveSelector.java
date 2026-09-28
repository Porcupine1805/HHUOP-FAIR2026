package org.hhuop.experiment;

import org.hhuop.model.*;
import java.util.*;

public final class SensitiveSelector {
    private SensitiveSelector(){}
    public static Set<Itemset> random(Set<Itemset> huops,double fraction,long seed){
        if(huops.isEmpty()) return Set.of();
        List<Itemset> list=new ArrayList<>(huops); Collections.sort(list); Collections.shuffle(list,new Random(seed));
        int k=Math.max(1,(int)Math.ceil(fraction*list.size()-1e-12)); k=Math.min(k,list.size());
        return new LinkedHashSet<>(list.subList(0,k));
    }
}
