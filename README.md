# OpenTrace

**OpenTrace** is a lightweight Java library that records a structured timeline of execution events (spans) in your code.
It captures nested operations, async execution, and context propagation to produce traces that can be inspected and analyzed. The goal of OpenTrace is to provide a **minimal tracing engine** with a simple API and zero external dependencies.


## features
* **span-based tracing**
  * record nested execution spans
  * automatic duration measurement

* **context propagation**
  * supports synchronous and asynchronous execution
  * thread and executor context propagation

* **distributed tracing**
  * HTTP client and server instrumentation
  * trace context injection and extraction

* **span metadata**
  * attributes
  * events
  * links
  * errors

* **baggage propagation**
  * propagate contextual metadata across services

* **metrics aggregation**
  * span latency statistics (avg, p95, p99)

* **export pipeline**
  * console exporter
  * extensible exporter interface


## build/test
```bash
mvn clean install
mvn -pl opentrace-core test
```


## recording errors
try-with-resources cannot see an exception thrown inside the block, so record it explicitly,
or use `tracer.trace(...)`, which captures and rethrows automatically:
```java
try(var span = tracer.span("charge-card")){
    try{ gateway.charge(); }
    catch(Exception e){ span.error(e); throw e; }
}

tracer.trace("charge-card", () -> gateway.charge());   // marks the span as ERROR on exception
```


## demos
`demo-app` contains runnable examples [not part of the library]:
- `demo.Demo` demonstrates nested spans, async propagation, events, links, baggage, attributes and errors.
- `demo.HttpDemo` demonstrates synchronous and asynchronous HTTP client instrumentation using a local HTTP server.
- `demo.JdbcDemo` demonstrates JDBC instrumentation using an embedded H2 database.
- `demo.ServletDemo` demonstrates servlet server instrumentation using `OpenTraceFilter` and embedded Tomcat.
- `demo.TraceAnnotationDemo` demonstrates interface-level and method-level `@Trace` annotations, custom span names, and nested proxy calls.

run the HTTP demo:
```bash
mvn -pl demo-app -am package
java -cp opentrace-core/target/classes:opentrace-http/target/classes:demo-app/target/classes demo.HttpDemo
```

run the JDBC demo:
```bash
mvn -pl demo-app -am install
mvn -f demo-app/pom.xml exec:java -Dexec.mainClass=demo.JdbcDemo
```

run the servlet demo:
```bash
mvn -pl demo-app -am install
mvn -f demo-app/pom.xml exec:java -Dexec.mainClass=demo.ServletDemo
```

run the annotation demo:
```bash
mvn -pl demo-app -am install
mvn -f demo-app/pom.xml exec:java -Dexec.mainClass=demo.TraceAnnotationDemo
```

## overhead
`demo.Bench` [a rough harness] measures what one span costs: no tracing, sampled out, sampled in without and with the trace store.

```bash
mvn clean install
java -cp opentrace-core/target/classes:demo-app/target/classes demo.Bench 1   # 1 thread
java -cp opentrace-core/target/classes:demo-app/target/classes demo.Bench 4   # 4 threads, shows contention
```
note: running it through `mvn exec:java` adds noise from maven's own JVM.


## OTLP exporter
for setup instructions and manual verification against an OpenTelemetry
collector, see [OTLP collector cerification](docs/otlp-collector.md).


```
NOTE: OpenTrace is currently an experimental tracing library intended for learning, experimentation, and lightweight observability.
```
