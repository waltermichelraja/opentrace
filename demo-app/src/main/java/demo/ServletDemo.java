package demo;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.processor.LoggingSpanProcessor;
import io.opentrace.servlet.OpenTraceFilter;

import jakarta.servlet.http.HttpServlet;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ServletDemo{

    public static void main(String[] args)throws Exception{
        OpenTrace tracer=OpenTrace.builder()
            .serviceName("servlet-demo")
            .serviceVersion("1.0.0")
            .environment("dev")
            .build();
        tracer.addProcessor(new LoggingSpanProcessor());
        Tomcat tomcat=new Tomcat();
        tomcat.setPort(0);
        try{
            Context context=tomcat.addContext(
                "",
                System.getProperty("java.io.tmpdir")
            );
            FilterDef filterDef=new FilterDef();
            filterDef.setFilterName("openTraceFilter");
            filterDef.setFilterClass(OpenTraceFilter.class.getName());
            filterDef.setFilter(new OpenTraceFilter(tracer));
            context.addFilterDef(filterDef);
            FilterMap filterMap=new FilterMap();
            filterMap.setFilterName("openTraceFilter");
            filterMap.addURLPattern("/*");
            context.addFilterMapBefore(filterMap);
            Tomcat.addServlet(
                context,
                "hello",
                new HttpServlet(){
                    @Override
                    protected void doGet(
                        jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response
                    )throws java.io.IOException{
                        response.setContentType("text/plain");
                        response.getWriter().write("Hello from OpenTrace Servlet!");
                    }
                }
            );
            context.addServletMappingDecoded("/hello","hello");
            tomcat.start();
            int port=tomcat.getConnector().getLocalPort();
            URI endpoint=URI.create(
                "http://127.0.0.1:"+port+"/hello"
            );
            HttpRequest request=HttpRequest.newBuilder()
                .uri(endpoint)
                .GET()
                .build();
            HttpResponse<String> response=HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("Response status: "+response.statusCode());
            System.out.println("Response body: "+response.body());
        }finally{
            tomcat.stop();
            tomcat.destroy();
            tracer.shutdown();
        }
    }
}