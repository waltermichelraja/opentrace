package demo;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.instrumentation.TracingProxy;
import io.opentrace.core.processor.LoggingSpanProcessor;

public class TraceAnnotationDemo{
    public static void main(String[] args){
        OpenTrace tracer=OpenTrace.builder()
            .serviceName("opentrace-annotation-demo")
            .build();

        tracer.addProcessor(new LoggingSpanProcessor());
        DemoService service=TracingProxy.create(
            new DemoServiceImpl(),
            tracer,
            DemoService.class
        );
        try(var root=tracer.root("annotation-demo")){
            service.validate();
        }
        tracer.shutdown();
    }
}