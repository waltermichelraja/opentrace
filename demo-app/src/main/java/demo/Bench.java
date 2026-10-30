package demo;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.sampling.AlwaysOffSampler;
import io.opentrace.core.sampling.AlwaysOnSampler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* rough overhead benchmark: cost of one span */
public class Bench{
    static final int CHILDREN=5;
    static final int REQUESTS_PER_ROUND=100_000;
    static final int WARMUP_ROUNDS=5;
    static final int MEASURED_ROUNDS=9;

    static volatile long sink;

    interface Scenario{ long run(int requests); }

    static long work(long x){ return x*31+7; }

    static long baseline(int requests){
        long acc=0;
        for(int i=0;i<requests;i++){
            acc=work(acc);
            for(int c=0;c<CHILDREN;c++){acc=work(acc);}
        }
        return acc;
    }

    static Scenario traced(OpenTrace tracer){
        return requests->{
            long acc=0;
            for(int i=0;i<requests;i++){
                try(var root=tracer.root("request")){
                    acc=work(acc);
                    for(int c=0;c<CHILDREN;c++){
                        try(var s=tracer.span("step")){acc=work(acc);}
                    }
                }
            }
            return acc;
        };
    }

    static void runThreads(Scenario scenario,int threads) throws InterruptedException{
        List<Thread> pool=new ArrayList<>();
        for(int t=0;t<threads;t++){
            Thread th=new Thread(()->{sink+=scenario.run(REQUESTS_PER_ROUND);});
            pool.add(th);
            th.start();
        }
        for(Thread th: pool){th.join();}
    }

    /** median nanoseconds per span, per thread, over the measured rounds */
    static double measure(Scenario scenario,int threads) throws InterruptedException{
        List<Double> samples=new ArrayList<>();
        for(int round=0;round<WARMUP_ROUNDS+MEASURED_ROUNDS;round++){
            long start=System.nanoTime();
            runThreads(scenario,threads);
            long elapsed=System.nanoTime()-start;
            if(round>=WARMUP_ROUNDS){
                samples.add((double)elapsed/((double)REQUESTS_PER_ROUND*(1+CHILDREN)));
            }
        }
        Collections.sort(samples);
        return samples.get(samples.size()/2);
    }

    public static void main(String[] args) throws Exception{
        int threads=args.length>0?Integer.parseInt(args[0]):1;

        OpenTrace off=OpenTrace.builder().sampler(new AlwaysOffSampler()).disableExporter().build();
        OpenTrace onNoStore=OpenTrace.builder().sampler(new AlwaysOnSampler()).disableExporter().maxStoredTraces(0).build();
        OpenTrace onDefault=OpenTrace.builder().sampler(new AlwaysOnSampler()).disableExporter().build();

        String[] names={
            "no tracing (baseline)",
            "sampled out (AlwaysOff)",
            "sampled in, no trace store",
            "sampled in, default store"
        };
        Scenario[] scenarios={
            Bench::baseline,
            traced(off),
            traced(onNoStore),
            traced(onDefault)
        };

        System.out.printf("java %s, %d thread(s), %d spans per request, %d requests per round%n",
            System.getProperty("java.version"),threads,1+CHILDREN,REQUESTS_PER_ROUND);
        System.out.printf("%-30s %12s %16s%n","scenario","ns per span","extra vs baseline");

        double base=0;
        for(int i=0;i<scenarios.length;i++){
            double ns=measure(scenarios[i],threads);
            if(i==0){base=ns;}
            System.out.printf("%-30s %12.1f %16.1f%n",names[i],ns,ns-base);
        }
        System.out.println("(sink="+sink+")");
    }
}