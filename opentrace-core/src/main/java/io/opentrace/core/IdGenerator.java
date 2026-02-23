package io.opentrace.core;

import java.util.concurrent.atomic.AtomicLong;

final class IdGenerator{
    private final AtomicLong counter=new AtomicLong(1);

    public long next(){
        return counter.getAndIncrement();
    }
}