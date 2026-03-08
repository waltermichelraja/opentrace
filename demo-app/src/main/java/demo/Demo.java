package demo;

import java.util.concurrent.CompletableFuture;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.instrumentation.TracingProxy;
import io.opentrace.core.SpanScope;
// import io.opentrace.core.sampling.AlwaysOnSampler;
// import io.opentrace.core.sampling.AlwaysOffSampler;

public class Demo{

    public static void main(String[] args){

        OpenTrace tracer=OpenTrace.builder()
            .samplingRate(1.0)
            .serviceName("testing")
            .environment("vsc-terminal")
            .serviceVersion("v0.3.0")
            .disableExporter()
            .build();

        DemoService target=new DemoServiceImpl();
        DemoService service=TracingProxy.create(target,tracer,DemoService.class);

        //    1. basic root + nested spans
        try(var root=tracer.root("order")){
            service.validate();
            service.validate();
        }

        //    2. async context propagation
        try(var root=tracer.root("test-span-async")){
            CompletableFuture.allOf(
                CompletableFuture.runAsync(tracer.wrap(() -> service.process())),
                CompletableFuture.runAsync(tracer.wrap(() -> service.validate()))
            ).join();
        }

        //    3. span events
        try(var root=tracer.root("test-span-events")){
            try(var span=tracer.span("validate")){
                span.addEvent("cache-miss");
                sleep(5);
                span.addEvent("db-call");
                sleep(5);
            }
        }

        //    4. span links
        try(var root=tracer.root("test-span-links")){
            try(var span=tracer.span("consumer")){
                span.addLink(100,108);
            }
        }

        //    5. baggage propagation
        try(var root=tracer.root("test-baggage")){
            tracer.putBaggage("user1Id","2403727715021059");

            CompletableFuture.runAsync(
                tracer.wrap(() ->
                    System.out.println("ASYNC user="+tracer.getBaggage("user1Id"))
                )
            ).join();

            System.out.println(tracer.getAllBaggage());
            sleep(20);
        }

        //    6. span attributes
        try(var root=tracer.root("test-span-attributes")){
            try(var span=tracer.span("db.query")){
                span.setAttribute("db.system","postgres");
                span.setAttribute("db.table","orders");
                span.setAttribute("db.operation","SELECT");
                sleep(10);
            }
        }

        //    7. error handling
        try(var root=tracer.root("test-error")){
            try(var span=tracer.span("dangerous-operation")){
                throw new RuntimeException("simulated failure");
            }catch(Exception e){
                // span automatically marked error in trace()
            }
        }

        //    8. trace wrapper test
        try(var root=tracer.root("test-trace-wrapper")){
            tracer.trace("custom-operation",()->{
                sleep(10);
            });
        }

        //    9. manual span control
        try(var root=tracer.root("manual-span")){
            SpanScope span=tracer.span("manual-work");
            sleep(10);
            span.close();
        }

        //    shutdown and export traces/metrics
        tracer.shutdown();

        System.out.println(tracer.getTraces());
        System.out.println(tracer.getMetrics());
    }

    private static void sleep(long ms){
        try{Thread.sleep(ms);}catch(Exception ignored){}
    }
}