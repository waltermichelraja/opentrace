package io.opentrace.core.sampling;

public final class AlwaysOffSampler implements Sampler{
    @Override
    public boolean shouldSample(){
        return false;
    }
}