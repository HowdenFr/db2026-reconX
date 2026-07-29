package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * ============================================================================
 * TICKET-ADV059 — DatabaseHealthIndicator (timed SELECT 1)
 *
 * WHAT:    Custom actuator HealthIndicator that runs a fast `SELECT 1` with
 *          a 2-second timeout and reports latencyMs as a detail.
 * HOW:     Extends AbstractHealthIndicator; Spring picks it up by bean name
 *          and exposes it under /actuator/health/database.
 * WHY:     The default DataSource health indicator works, but a custom one
 *          gives us a controllable timeout AND visible latency for SRE
 *          dashboards.
 * ============================================================================
 */

//Component For Spring 
@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {
    //Contains a Final DataSorce
    private final DataSource ds;

    public DatabaseHealthIndicator(DataSource ds) {
        
        this.ds = ds;
    }
    
    //Checking Connection From the DataBase | Builder Stores Information From Heck 
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        //Gives the current time 
        long start = System.nanoTime();
        //Try/Catch for Query -- 2 Seconds is the Time 
        try (Connection connection = ds.getConnection();
             Statement statement = connection.createStatement()) {

            statement.setQueryTimeout(2);
            statement.execute("SELECT 1");
             
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            
            //This is What is Actually Be Put wihtin our builder( based on health Check)
            builder.up()
                    .withDetail("query", "SELECT 1")
                    .withDetail("latencyMs", latencyMs);

        } catch (Exception e) {
            builder.down(e)
                    .withDetail("query", "SELECT 1");
        }
    }
}