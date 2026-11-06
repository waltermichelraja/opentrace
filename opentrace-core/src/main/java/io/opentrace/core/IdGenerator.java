package io.opentrace.core;

import java.util.concurrent.ThreadLocalRandom;

final class IdGenerator{
    TraceId nextTraceId(){
        ThreadLocalRandom random=ThreadLocalRandom.current();
        long high,low;
        do{
            high=random.nextLong();
            low=random.nextLong();
        }while(high==0L && low==0L);
        return new TraceId(high,low);
    }

    long nextSpanId(){
        ThreadLocalRandom random=ThreadLocalRandom.current();
        long id;
        do{
            id=random.nextLong();
        }while(id==0L);
        return id;
    }
}