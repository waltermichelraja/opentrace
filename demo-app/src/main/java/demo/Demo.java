package demo;

import io.opentrace.core.OpenTrace;

public class Demo{
    public static void main(String[] args){
        OpenTrace tracer=OpenTrace.builder().build();
        
        tracer.startRoot("order");

        tracer.startSpan("validate");
        sleep(20);
        tracer.endSpan();

        tracer.startSpan("payment");
        sleep(50);
        tracer.endSpan();

        tracer.endRoot();

        sleep(2000);
        tracer.shutdown();
    }

    static void sleep(long ms){
        try{
            Thread.sleep(ms);
        }catch(Exception ignored){}
    }
}