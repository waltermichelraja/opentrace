package io.opentrace.core.instrumentation;

public interface SelfAware<T>{
    void setSelf(T self);
}