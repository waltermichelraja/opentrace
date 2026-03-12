package io.opentrace.core.processor;

import io.opentrace.core.Trace;
import io.opentrace.core.exporter.SpanExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
        queue.offer(trace);
    }

    private void runWorker(){
        try{
            while(running || !queue.isEmpty()){
                List<Trace> batch=new ArrayList<>(batchSize);
                queue.drainTo(batch,batchSize);
                if(batch.isEmpty()){
                    Trace t=queue.poll(500,TimeUnit.MILLISECONDS);
                    if(t!=null){batch.add(t);}
                }
                if(!batch.isEmpty()){exporter.export(batch);}
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
    }

    @Override
    public void shutdown(){
        running=false;
        worker.interrupt();
    }
}