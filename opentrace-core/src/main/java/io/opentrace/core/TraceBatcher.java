package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

    void submit(Trace trace){queue.offer(trace);}

    private void runWorker(){
        while(running){
            try{
                List<Trace> batch=new ArrayList<>(batchSize);
                queue.drainTo(batch,batchSize);
                if(batch.isEmpty()){
                    Trace t=queue.poll(1,TimeUnit.SECONDS);
                    if(t!=null){batch.add(t);}
                }
                if(!batch.isEmpty()){exporter.export(batch);}

            }catch(Exception ignored){}
        }
    }

    void shutdown(){
        running=false;
        worker.interrupt();
    }
}