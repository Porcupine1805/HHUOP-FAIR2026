package org.hhuop.experiment;
import java.util.*;
public final class Args {
    private final Map<String,String> m=new HashMap<>();
    public Args(String[] args){for(int i=0;i<args.length;i++){String a=args[i];if(a.startsWith("--")){String k=a.substring(2);String v=(i+1<args.length&&!args[i+1].startsWith("--"))?args[++i]:"true";m.put(k,v);}}}
    public String get(String k,String d){return m.getOrDefault(k,d);} public int getInt(String k,int d){return Integer.parseInt(get(k,Integer.toString(d)));}
    public long getLong(String k,long d){return Long.parseLong(get(k,Long.toString(d)));} public double getDouble(String k,double d){return Double.parseDouble(get(k,Double.toString(d)));}
    public boolean has(String k){return m.containsKey(k);}
}
