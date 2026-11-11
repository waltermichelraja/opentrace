# verify OpenTrace with an OpenTelemetry collector

this guide explains how to send OpenTrace spans to a locally running OpenTelemetry Collector using OTLP/HTTP JSON.


## prerequisites
- docker installed and running
- java 17 or later
- OpenTrace built locally


## start the OpenTelemetry collector
from the repository root:
```bash
docker run --rm \
  -p 4318:4318 \
  -v "$(pwd)/docs/otel-collector-config.yaml:/etc/otelcol-contrib/config.yaml" \
  otel/opentelemetry-collector-contrib:latest \
  --config=/etc/otelcol-contrib/config.yaml
```
the collector listens for OTLP/HTTP requests on port `4318` and prints received traces through its debug exporter.


## configure OpenTrace
create an exporter using the collector's OTLP/HTTP traces endpoint:
```java
import io.opentrace.core.OpenTrace;
import io.opentrace.core.exporter.OtlpExporter;

public class OtlpDemo{
    public static void main(String[] args){
        OpenTrace tracer=OpenTrace.builder()
            .serviceName("opentrace-demo")
            .serviceVersion("1.0.0")
            .environment("local")
            .exporter(new OtlpExporter("http://localhost:4318/v1/traces"))
            .build();

        try(var root=tracer.root("checkout")){
            try(var span=tracer.span("database-query")){
                span.setAttribute("db.system","postgresql");
                span.addEvent("query-executed");
            }
        }

        tracer.shutdown();
    }
}
```
the shutdown call allows the batch processor to flush pending traces before the application exits.


## verify the output
inspect the collector's terminal output:
- resource attributes such as `service.name`, `service.version`, and `deployment.environment`.
- `checkout` root span.
- `database-query` child span.
- `db.system` span attribute.
- `query-executed` event.
- trace IDs, span IDs, parent span IDs, and timestamps.


## troubleshooting

**connection refused:** ensure Docker is running and port `4318` is available.

**no traces appear:** confirm that the exporter endpoint ends with `/v1/traces` and that `tracer.shutdown()` is called before the application exits.

**collector rejects the request:** check the collector configuration and inspect its terminal output for OTLP receiver errors.

**no output after sending traces:** verify that the collector's traces pipeline includes both the `otlp` receiver and the `debug` exporter.


```
NOTE: this guide uses a local collector with a debug exporter. It is intended for development and manual verification, not production deployment.
```