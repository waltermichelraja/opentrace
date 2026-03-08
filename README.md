# OpenTrace

**OpenTrace** is a lightweight Java library that records a structured timeline of execution events (spans) in your code.
It captures nested operations, async execution, and context propagation to produce traces that can be inspected and analyzed. The goal of OpenTrace is to provide a **minimal tracing engine** with a simple API and zero external dependencies.


## Features
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


## Example
```java
OpenTrace tracer = OpenTrace.builder()
    .serviceName("order-service")
    .environment("dev")
    .serviceVersion("1.0")
    .build();

try(var root = tracer.root("order")){
    try(var span = tracer.span("validate")){
        span.setAttribute("userId",42);
    }

    try(var span = tracer.span("process")){
        // business logic
    }
}
```


```
STATUS/NOTE: OpenTrace is currently an experimental tracing library intended for learning, experimentation, and lightweight observability.
```
