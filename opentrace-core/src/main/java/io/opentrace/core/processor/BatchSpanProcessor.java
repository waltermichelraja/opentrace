package io.opentrace.core.processor;

import io.opentrace.core.Trace;
import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class BatchSpanProcessor implements SpanProcessor{
    private final BlockingQueue<Trace> queue;
    private final SpanExporter exporter;
    private final int batchSize;
    private final Object lifecycleLock=new Object();

    private final AtomicLong exported=new AtomicLong();
    private final AtomicLong dropped=new AtomicLong();
    private final AtomicLong failed=new AtomicLong();
    private volatile String lastFailureType;
    private volatile String lastFailureMessage;

    private volatile boolean running=true;
    private boolean accepting=true;
    private final Thread worker;

    public BatchSpanProcessor(int batchSize,int queueCapacity,SpanExporter exporter){
        this.queue=new LinkedBlockingQueue<>(queueCapacity);
        this.exporter=exporter;
        this.batchSize=batchSize;
        this.worker=new Thread(this::runWorker,"opentrace-batch-processor");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void onEnd(Trace trace){
        synchronized(lifecycleLock){
            if(!accepting){
                dropped.incrementAndGet();
                return;
            }
            if(!queue.offer(trace)){
                long n=dropped.incrementAndGet();
                if(n==1||n%1000==0){
                    System.err.println("opentrace: export queue full, "+n+" trace(s) dropped so far");
                }
            }
        }
    }

    public ExportStats stats(){
        return new ExportStats(
            exported.get(),
            dropped.get(),
            failed.get(),
            queue.size(),
            queue.size()+queue.remainingCapacity(),
            worker.isAlive(),
            lastFailureType,
            lastFailureMessage
        );
    }

    private void runWorker(){
        boolean interrupted=false;
        while(running||!queue.isEmpty()){
            try{
                List<Trace> batch=new ArrayList<>(batchSize);
                queue.drainTo(batch,batchSize);
                if(batch.isEmpty()){
                    Trace trace=queue.poll(100,TimeUnit.MILLISECONDS);
                    if(trace!=null){batch.add(trace);}
                }
                export(batch);
            }catch(InterruptedException e){
                interrupted=true;
            }
        }
        if(interrupted){
            Thread.currentThread().interrupt();
        }
    }

    private void export(List<Trace> batch){
        if(batch.isEmpty()){return;}
        try{
            exporter.export(batch);
            exported.addAndGet(batch.size());
        }catch(RuntimeException e){
            failed.addAndGet(batch.size());
            lastFailureType=e.getClass().getSimpleName();
            lastFailureMessage=e.getMessage();
            System.err.println("opentrace: exporter failed: "+e);
        }
    }

    @Override
    public void shutdown(){
        synchronized(lifecycleLock){
            accepting=false;
            running=false;
        }
        try{
            worker.join(5000);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}