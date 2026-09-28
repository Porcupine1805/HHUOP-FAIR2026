package org.hhuop.experiment;

import org.hhuop.io.DatabaseReader;
import org.hhuop.mining.*;
import org.hhuop.model.*;
import org.hhuop.sanitize.*;
import org.hhuop.util.MemorySampler;
import java.nio.file.*;
import java.util.*;

public final class ExperimentMain {
    public static void main(String[] argv) throws Exception {
        Args a=new Args(argv);
        Path dbPath=Path.of(a.get("db","datasets/example/database.txt"));
        Path profitPath=Path.of(a.get("profits","datasets/example/profits.txt"));
        double alpha=a.getDouble("alpha",0.4), beta=a.getDouble("beta",0.78);
        double sensitiveFraction=a.getDouble("sensitiveFraction",0.5);
        long seed=a.getLong("seed",42);
        double epsilon=a.getDouble("epsilon",0.001), borderEta=a.getDouble("borderEta",0.05);
        double wM=a.getDouble("wM",1), wA=a.getDouble("wA",1), wD=a.getDouble("wD",0.1), wS=a.getDouble("wS",0.5);
        int maxActions=a.getInt("maxActions",100000);
        Path out=Path.of(a.get("out","results/results.csv"));
        Path actionDir=Path.of(a.get("actionDir","results/actions"));

        validate(alpha,beta,sensitiveFraction,epsilon,borderEta,maxActions);
        if(!Double.isFinite(wM)||wM<0||!Double.isFinite(wA)||wA<0||!Double.isFinite(wD)||wD<0||!Double.isFinite(wS)||wS<0)
            throw new IllegalArgumentException("SMSE weights must be finite and >= 0");
        QuantitativeDatabase db=DatabaseReader.read(dbPath,profitPath);
        HUOPMiner miner=new VerticalHUOPMiner();
        HUOPMiningResult initial;double initialPeakMB;
        try(MemorySampler sampler=new MemorySampler()){initial=miner.mine(db,alpha,beta);initialPeakMB=sampler.peakMB();}
        Set<Itemset> sensitive;
        if(a.has("sensitiveFile")) sensitive=DatabaseReader.readItemsets(Path.of(a.get("sensitiveFile","")));
        else sensitive=SensitiveSelector.random(initial.itemsets(),sensitiveFraction,seed);
        if(sensitive.isEmpty()) throw new IllegalStateException("No HUOPs found / no sensitive set selected. Adjust alpha/beta.");
        for(Itemset x:sensitive) if(!initial.itemsets().contains(x)) throw new IllegalArgumentException("Sensitive itemset is not HUOP: "+x);

        List<AbstractHHUOPSanitizer> algs=List.of(
                new HHUOPSMAU(miner,epsilon,maxActions,borderEta),
                new HHUOPSMIU(miner,epsilon,maxActions,borderEta),
                new HHUOPSMSE(miner,epsilon,maxActions,borderEta,wM,wA,wD,wS),
                new HHUOPDEL(miner,epsilon,maxActions,borderEta),
                new HHUOPHUI(miner,epsilon,maxActions,borderEta),
                new HHUOPGRED(miner,epsilon,maxActions,borderEta));
        Set<String> requested=new LinkedHashSet<>();
        for(String token:a.get("algorithms","SMAU,SMIU,SMSE").toUpperCase(Locale.ROOT).split(",")){
            String name=token.trim();if(!Set.of("SMAU","SMIU","SMSE","DEL","HUI","GRED").contains(name))throw new IllegalArgumentException("Unknown algorithm: "+name);
            requested.add(name);
        }

        Files.createDirectories(out.toAbsolutePath().getParent()); Files.createDirectories(actionDir);
        List<String> rows=new ArrayList<>(); SanitizationMetrics first=null;
        for(AbstractHHUOPSanitizer alg:algs){
            String shortName=alg.name().replace("HHUOP-",""); if(!requested.contains(shortName)) continue;
            SanitizationResult r=alg.sanitize(db,alpha,beta,sensitive,initial,initial.runtimeMs(),initialPeakMB);
            SanitizationMetrics metadata=r.metrics();metadata.dataset=dbPath.getParent()==null?dbPath.toString():dbPath.getParent().getFileName().toString();
            metadata.alpha=alpha;metadata.beta=beta;metadata.sensitiveFraction=a.has("sensitiveFile")?Double.NaN:sensitiveFraction;metadata.seed=seed;
            if("SMSE".equals(shortName)){metadata.wM=wM;metadata.wA=wA;metadata.wD=wD;metadata.wS=wS;}
            if(first==null){first=r.metrics();rows.add(first.csvHeader());}
            rows.add(r.metrics().toCsv());
            List<String> actions=new ArrayList<>();
            actions.add("step,algorithm,target,tid,item,beforeQuantity,afterQuantity,utilityLoss,predictedMissing,predictedArtificial,score");
            for(SanitizationAction x:r.actions()) actions.add(x.toString());
            Files.write(actionDir.resolve(shortName.toLowerCase(Locale.ROOT)+"_actions.csv"),actions);
            System.out.println(r.metrics().toCsv());
        }
        Files.write(out,rows);
        if(a.has("verbosePatterns")){System.out.println("Initial HUOPs: "+initial.itemsets());System.out.println("Sensitive: "+sensitive);}
        else System.out.println("Initial HUOP count: "+initial.itemsets().size()+"; sensitive count: "+sensitive.size());
        System.out.println("Results: "+out.toAbsolutePath());
    }
    private static void validate(double alpha,double beta,double fraction,double epsilon,double borderEta,int maxActions){
        if(!Double.isFinite(alpha)||alpha<=0||alpha>1)throw new IllegalArgumentException("alpha must be in (0,1]");
        if(!Double.isFinite(beta)||beta<0||beta>1)throw new IllegalArgumentException("beta must be in [0,1]");
        if(!Double.isFinite(fraction)||fraction<=0||fraction>1)throw new IllegalArgumentException("sensitiveFraction must be in (0,1]");
        if(!Double.isFinite(epsilon)||epsilon<0)throw new IllegalArgumentException("epsilon must be finite and >= 0");
        if(!Double.isFinite(borderEta)||borderEta<0)throw new IllegalArgumentException("borderEta must be finite and >= 0");
        if(maxActions<=0)throw new IllegalArgumentException("maxActions must be > 0");
    }
}
