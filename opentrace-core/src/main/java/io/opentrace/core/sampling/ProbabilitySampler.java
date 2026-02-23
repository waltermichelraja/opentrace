package io.opentrace.core.sampling;

import java.util.concurrent.ThreadLocalRandom;

public final class ProbabilitySampler implements Sampler{
    private final double rate;

    public ProbabilitySampler(double rate){
        if(rate<0.0 || rate>1.0){throw new IllegalArgumentException("samplingRate must be between 0.0 and 1.0");}
        this.rate=rate;
    }

    @Override
    public boolean shouldSample(){
        return ThreadLocalRandom.current().nextDouble()<rate;
    }
}