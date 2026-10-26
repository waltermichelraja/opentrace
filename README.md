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
`demo-app` contains runnable examples [not part of the library]: `demo.Demo` walks through nested spans, async
propagation, events, links, baggage, attributes and errors; `demo.HttpDemo` starts a local server on :8080
(`curl localhost:8080/call`) to show trace propagation across an HTTP call.


```
STATUS/NOTE: OpenTrace is currently an experimental tracing library intended for learning, experimentation, and lightweight observability.
```
