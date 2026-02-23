package io.opentrace.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class NameRegistry{
    private final ConcurrentHashMap<String,Integer> nameToId=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer,String> idToName=new ConcurrentHashMap<>();
    private final AtomicInteger counter=new AtomicInteger(1);

    int id(String name){
        return nameToId.computeIfAbsent(name,k->{
            int id=counter.getAndIncrement();
            idToName.put(id,k);
            return id;
        });
    }

    String name(int id){return idToName.get(id);}
}