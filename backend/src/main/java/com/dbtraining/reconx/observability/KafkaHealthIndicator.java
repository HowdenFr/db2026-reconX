package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component("reconxKafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaHealthIndicator extends AbstractHealthIndicator {
   //Boot Trap Server Object
    private final String bootstrapServers;
     
    //Constructor, Implements Super Method From AbstractHealthIndicator 
    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers) {
        super("ReconX Kafka health check failed");
        this.bootstrapServers = bootstrapServers;
    }
     
    //Builder | Contains Information From Our Kafka Health Check 
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                2_000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
                3_000
        ); 

        //This Will Try our Connection to the Kafka Cluster

        try (AdminClient adminClient = AdminClient.create(config)) {
            //Clster Object 
            DescribeClusterResult cluster = adminClient.describeCluster();

            /*
            This is Information get From Our Cluster | Node Count | etc..
            
            */
            //Cluster ID
            String clusterId = cluster.clusterId()
                    .get(2, TimeUnit.SECONDS);
           
            int nodeCount = cluster.nodes()
                    .get(2, TimeUnit.SECONDS)
                    .size();

            builder.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount);

        } catch (Exception exception) {
            builder.down(exception);
        }
    }
}