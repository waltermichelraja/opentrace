package demo;

import io.opentrace.core.instrumentation.SelfAware;
import io.opentrace.core.instrumentation.Trace;

public class DemoServiceImpl implements DemoService, SelfAware<DemoService>{

    private DemoService self;

    @Override
    public void setSelf(DemoService self){
        this.self=self;
    }

    @Override
    public void validate(){
        self.process();
    }

    @Override
    @Trace("service.process")
    public void process(){
        try{Thread.sleep(20);}catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Service processing was interrupted",e);
        }
    }
}