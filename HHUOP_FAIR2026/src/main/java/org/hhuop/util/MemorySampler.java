package org.hhuop.util;

public final class MemorySampler implements AutoCloseable {
    private volatile boolean running = true;
    private volatile long peak = 0;
    private final Thread thread;
    public MemorySampler() {
        sample();
        thread = new Thread(() -> {
            while(running){
                sample();
                try{Thread.sleep(2);}catch(InterruptedException ignored){Thread.currentThread().interrupt();break;}
            }
        },"memory-sampler");
        thread.setDaemon(true); thread.start();
    }
    public double peakMB(){ return peak/(1024.0*1024.0); }
    private void sample(){Runtime rt=Runtime.getRuntime();long used=rt.totalMemory()-rt.freeMemory();if(used>peak)peak=used;}
    @Override public void close(){ running=false; thread.interrupt(); try{thread.join();}catch(InterruptedException ignored){Thread.currentThread().interrupt();} sample(); }
}
