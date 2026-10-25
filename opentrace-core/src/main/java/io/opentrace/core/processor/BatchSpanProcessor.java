package io.opentrace.core.processor;

import io.opentrace.core.Trace;
import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class BatchSpanProcessor implements SpanProcessor{
    private final BlockingQueue<Trace> queue;
    private final SpanExporter exporter;
    private final int batchSize;

    private volatile boolean running=true;
    private final Thread worker;

    public BatchSpanProcessor(int batchSize,int queueCapacity, SpanExporter exporter){
        this.queue=new LinkedBlockingQueue<>(queueCapacity);
        this.exporter=exporter;
        this.batchSize=batchSize;
        this.worker=new Thread(this::runWorker,"opentrace-batch-processor");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void onEnd(Trace trace){
        if(!queue.offer(trace)){
            System.err.println("opentrace: export queue full, dropping trace "+trace.traceId);
        }
    }

    private void runWorker(){
        try{
            while(running || !queue.isEmpty()){
                List<Trace> batch=new ArrayList<>(batchSize);
                queue.drainTo(batch,batchSize);
                if(batch.isEmpty()){
                    Trace t=queue.poll(100,TimeUnit.MILLISECONDS);
                    if(t!=null){batch.add(t);}
                }
                export(batch);
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void export(List<Trace> batch){
        if(batch.isEmpty()){return;}
        try{
            exporter.export(batch);
        }catch(RuntimeException e){
            System.err.println("opentrace: exporter failed: "+e);
        }
    }

    @Override
    public void shutdown(){
        running=false;
        try{worker.join(5000);}
        catch(InterruptedException e){Thread.currentThread().interrupt();}
        List<Trace> rest=new ArrayList<>();
        queue.drainTo(rest);
        export(rest);
    }
}