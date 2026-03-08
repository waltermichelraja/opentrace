package demo;

import io.opentrace.core.instrumentation.Trace;

@Trace
public interface DemoService{
    void validate();
    void process();
}