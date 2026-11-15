package io.opentrace.jdbc;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.Span;
import io.opentrace.core.SpanStatus;
import io.opentrace.core.Trace;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TraceJdbcIntegrationTest{

    @Test
    void realDatabaseExecutesStatementAndPreparedStatement()throws Exception{
        OpenTrace tracer=OpenTrace.builder().build();
        DataSource traced=tracedDataSource(tracer);

        try(var root=tracer.root("jdbc.integration")){
            try(Connection connection=traced.getConnection()){
                try(Statement statement=connection.createStatement()){
                    statement.executeUpdate(
                        "CREATE TABLE users(id INT PRIMARY KEY,name VARCHAR(100))"
                    );
                }

                try(PreparedStatement statement=connection.prepareStatement(
                    "INSERT INTO users(id,name) VALUES(?,?)"
                )){
                    statement.setInt(1,1);
                    statement.setString(2,"Alice");
                    assertEquals(1,statement.executeUpdate());
                }

                try(PreparedStatement statement=connection.prepareStatement(
                    "SELECT name FROM users WHERE id=?"
                )){
                    statement.setInt(1,1);

                    try(ResultSet result=statement.executeQuery()){
                        assertTrue(result.next());
                        assertEquals("Alice",result.getString("name"));
                    }
                }
            }
        }

        Trace trace=onlyTrace(tracer);

        assertEquals(4,trace.spans.size());
        assertEquals(3,trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals("db.query"))
            .count());

        assertTrue(trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals("db.query"))
            .allMatch(span->"jdbc".equals(span.getAttributes().get("db.system"))));
    }

    @Test
    void realDatabaseFailureRecordsSqlError()throws Exception{
        OpenTrace tracer=OpenTrace.builder().build();
        DataSource traced=tracedDataSource(tracer);

        try(var root=tracer.root("jdbc.integration")){
            try(Connection connection=traced.getConnection();
                Statement statement=connection.createStatement()){

                assertThrows(SQLException.class,
                    ()->statement.executeQuery("SELECT * FROM missing_table"));
            }
        }

        Trace trace=onlyTrace(tracer);
        Span span=findSpan(trace,"db.query");

        assertEquals(SpanStatus.ERROR,span.getStatus());
        assertNotNull(span.getAttributes().get("error.type"));
        assertNotNull(span.getAttributes().get("db.response.sqlstate"));
    }

    private static DataSource tracedDataSource(OpenTrace tracer){
        JdbcDataSource source=new JdbcDataSource();
        source.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        return TraceJdbc.wrap(source,tracer);
    }

    private static Trace onlyTrace(OpenTrace tracer){
        assertEquals(1,tracer.getTraceList().size());
        return tracer.getTraceList().get(0);
    }

    private static Span findSpan(Trace trace,String name){
        return trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals(name))
            .findFirst()
            .orElseThrow();
    }
}