package org.hhuop.sanitize;

public final class SanitizationMetrics {
    public String dataset="";
    public double alpha,beta,sensitiveFraction;
    public long seed;
    public String algorithm;
    public int originalHUOPs, sensitiveCount, finalHUOPs;
    public int hidingFailures, missingCount, artificialCount, actions;
    public double hf, mc, ac, dus, utilityLoss, od, tdr, qdr;
    public double initialMiningMs, setupMs, sanitizationMs, verificationMs, endToEndSanitizationMs, totalMs, peakMemoryMB;
    public double wM=Double.NaN, wA=Double.NaN, wD=Double.NaN, wS=Double.NaN;

    public String csvHeader() {
        return "dataset,alpha,beta,sensitiveFraction,seed,algorithm,originalHUOPs,sensitiveCount,finalHUOPs,hidingFailures,missingCount,artificialCount,actions,"+
                "HF,MC,AC,DUS,utilityLoss,OD,TDR,QDR,initialMiningMs,setupMs,actionSelectionMs,verificationMs,endToEndSanitizationMs,totalMs,peakMemoryMB,wM,wA,wD,wS";
    }
    public String toCsv() {
        return String.join(",",
                csv(dataset),f(alpha),f(beta),f(sensitiveFraction),Long.toString(seed),algorithm,
                Integer.toString(originalHUOPs),Integer.toString(sensitiveCount),Integer.toString(finalHUOPs),
                Integer.toString(hidingFailures),Integer.toString(missingCount),Integer.toString(artificialCount),Integer.toString(actions),
                f(hf),f(mc),f(ac),f(dus),f(utilityLoss),f(od),f(tdr),f(qdr),
                f(initialMiningMs),f(setupMs),f(sanitizationMs),f(verificationMs),f(endToEndSanitizationMs),f(totalMs),f(peakMemoryMB),
                f(wM),f(wA),f(wD),f(wS));
    }
    private static String f(double x){ return String.format(java.util.Locale.US,"%.8f",x); }
    private static String csv(String x){return "\""+x.replace("\"","\"\"")+"\"";}
}
