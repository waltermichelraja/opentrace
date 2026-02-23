package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

final class TraceBatcher{
    private final BlockingQueue<Trace> queue;
    private final SpanExporter exporter;
    private final int batchSize;
    private volatile boolean running=true;
    private final Thread worker;

    TraceBatcher(OpenTraceConfig config){
        this.queue=new LinkedBlockingQueue<>(config.queueCapacity);
        this.exporter=config.exporter;
        this.batchSize=config.batchSize;
        this.worker=new Thread(this::runWorker,"opentrace-worker");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    void submit(Trace trace){
        queue.offer(trace);
    }

    private void runWorker(){
        try{
            while(running || !queue.isEmpty()){
                List<Trace> batch=new ArrayList<>(batchSize);
                queue.drainTo(batch,batchSize);
                if(batch.isEmpty()){
                    Trace t=queue.poll(500,TimeUnit.MILLISECONDS);
                    if(t!=null){
                        batch.add(t);
                    }
                }
                if(!batch.isEmpty()){exporter.export(batch);}
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }finally{flushRemaining();}
    }

    private void flushRemaining(){
        List<Trace> remaining=new ArrayList<>();
        queue.drainTo(remaining);
        if(!remaining.isEmpty()){
            exporter.export(remaining);
        }
    }

    void shutdown(){
        running=false;
        worker.interrupt();
        try{
            worker.join(2000);
        }catch(InterruptedException ignored){
            Thread.currentThread().interrupt();
        }
    }
}