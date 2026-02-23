package demo;

import io.opentrace.core.OpenTrace;

public class Demo{
    public static void main(String[] args){
        OpenTrace tracer=OpenTrace.builder().build();

        try(var root=tracer.root("order")){
            try(var span=tracer.span("validate")){
                sleep(20);
            }
            try(var span=tracer.span("payment")){
                sleep(50);
            }
        }
        sleep(2000);
        tracer.shutdown();
    }

    static void sleep(long ms){
        try{
            Thread.sleep(ms);
        }catch(Exception ignored){}
    }
}