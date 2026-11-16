package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenTraceConfigTest{
    @Test
    void builderUsesExpectedDefaultResourceValues(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){}
            var resource=tracer.getTraceList().get(0).resource;
            assertEquals("unknown-service",resource.getAttributes().get("service.name"));
            assertEquals("dev",resource.getAttributes().get("deployment.environment"));
            assertEquals("0.0.1",resource.getAttributes().get("service.version"));
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void buildingCapturesConfigurationIndependently(){
        var builder=OpenTrace.builder()
            .serviceName("service-one")
            .environment("test")
            .serviceVersion("1.0.0")
            .disableExporter();
        var first=builder.build();
        builder.serviceName("service-two")
            .environment("production")
            .serviceVersion("2.0.0");
        var second=builder.build();
        try{
            try(var root=first.root("first")){}
            try(var root=second.root("second")){}
            Map<String,String> firstAttributes=
                first.getTraceList().get(0).resource.getAttributes();
            Map<String,String> secondAttributes=
                second.getTraceList().get(0).resource.getAttributes();
            assertEquals("service-one",firstAttributes.get("service.name"));
            assertEquals("test",firstAttributes.get("deployment.environment"));
            assertEquals("1.0.0",firstAttributes.get("service.version"));

            assertEquals("service-two",secondAttributes.get("service.name"));
            assertEquals("production",secondAttributes.get("deployment.environment"));
            assertEquals("2.0.0",secondAttributes.get("service.version"));
        }finally{
            first.shutdown();
            second.shutdown();
        }
    }

    @Test
    void resourceAttributesCannotBeModified(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("root")){}
            var attributes=tracer.getTraceList().get(0).resource.getAttributes();
            assertThrows(
                UnsupportedOperationException.class,
                ()->attributes.put("service.name","modified")
            );
        }finally{
            tracer.shutdown();
        }
    }
}
