package demo;

import io.opentrace.core.instrumentation.SelfAware;

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
    public void process(){
        try{Thread.sleep(20);}catch(Exception ignored){}
    }
}