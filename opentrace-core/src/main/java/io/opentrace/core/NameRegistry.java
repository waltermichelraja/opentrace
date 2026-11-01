package io.opentrace.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class NameRegistry{
    static final String OVERFLOW_NAME="_other";

    private final ConcurrentHashMap<String,Integer> nameToId=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer,String> idToName=new ConcurrentHashMap<>();
    private final AtomicInteger counter=new AtomicInteger(1);
    private final AtomicBoolean warned=new AtomicBoolean();
    private final int maxNames;
    private final int overflowId;

    NameRegistry(int maxNames){
        if(maxNames<=0){throw new IllegalArgumentException("maxNames must be positive");}
        this.maxNames=maxNames;
        this.overflowId=counter.getAndIncrement();
        idToName.put(overflowId,OVERFLOW_NAME);
    }

    int id(String name){
        Integer existing=nameToId.get(name);
        if(existing!=null){return existing;}
        if(nameToId.size()>=maxNames){
            if(warned.compareAndSet(false,true)){
                System.err.println("opentrace: more than "+maxNames+" distinct span names; further names are recorded as '"
                    +OVERFLOW_NAME+"'. Put ids and other variable data in attributes, not in span names.");
            }
            return overflowId;
        }
        return nameToId.computeIfAbsent(name,k->{
            int id=counter.getAndIncrement();
            idToName.put(id,k);
            return id;
        });
    }

    String name(int id){return idToName.get(id);}
}