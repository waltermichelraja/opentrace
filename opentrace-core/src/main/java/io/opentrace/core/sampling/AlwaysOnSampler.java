package io.opentrace.core.sampling;

public final class AlwaysOnSampler implements Sampler{
    @Override
    public boolean shouldSample(){
        return true;
    }
}