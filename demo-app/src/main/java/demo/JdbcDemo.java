package demo;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.processor.LoggingSpanProcessor;
import io.opentrace.jdbc.TraceJdbc;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcDemo{

    public static void main(String[] args)throws Exception{
        OpenTrace tracer=OpenTrace.builder()
            .serviceName("jdbc-demo")
            .serviceVersion("1.0.0")
            .environment("dev")
            .build();

        tracer.addProcessor(new LoggingSpanProcessor());

        JdbcDataSource source=new JdbcDataSource();
        source.setURL("jdbc:h2:mem:opentrace_demo;DB_CLOSE_DELAY=-1");

        DataSource traced=TraceJdbc.wrap(source,tracer);

        try(var root=tracer.root("jdbc-demo")){
            try(Connection connection=traced.getConnection()){
                try(Statement statement=connection.createStatement()){
                    statement.executeUpdate(
                        "CREATE TABLE products(id INT PRIMARY KEY,name VARCHAR(100))"
                    );
                }

                try(PreparedStatement statement=connection.prepareStatement(
                    "INSERT INTO products(id,name) VALUES(?,?)"
                )){
                    statement.setInt(1,1);
                    statement.setString(2,"OpenTrace");

                    statement.executeUpdate();
                }

                try(PreparedStatement statement=connection.prepareStatement(
                    "SELECT name FROM products WHERE id=?"
                )){
                    statement.setInt(1,1);

                    try(ResultSet result=statement.executeQuery()){
                        if(result.next()){
                            System.out.println("Product: "+result.getString("name"));
                        }
                    }
                }
            }
        }
        tracer.shutdown();
    }
}
