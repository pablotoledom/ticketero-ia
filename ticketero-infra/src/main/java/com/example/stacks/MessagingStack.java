package com.example.stacks;

import com.example.config.EnvironmentConfig;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.amazonmq.CfnBroker;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.List;

/**
 * Stack de mensajeria: Amazon MQ (RabbitMQ).
 * Depende de NetworkStack para VPC y Security Groups.
 */
public class MessagingStack extends Stack {

    private final CfnBroker broker;
    private final Secret mqSecret;
    private final String mqUsername = "ticketero";

    public MessagingStack(final Construct scope, final String id,
                          final StackProps props, final EnvironmentConfig config,
                          final NetworkStack networkStack) {
        super(scope, id, props);

        String prefix = config.resourcePrefix();

        // ========================================
        // Secret - Credenciales de RabbitMQ
        // ========================================
        this.mqSecret = Secret.Builder.create(this, "MqSecret")
            .secretName(prefix + "-mq-credentials")
            .description("Credenciales para Amazon MQ RabbitMQ")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"username\": \"" + mqUsername + "\"}")
                .generateStringKey("password")
                .excludePunctuation(true)
                .passwordLength(32)
                .build())
            .build();

        // ========================================
        // Obtener subnets privadas
        // ========================================
        List<String> subnetIds = networkStack.getVpc()
            .getPrivateSubnets()
            .stream()
            .map(subnet -> subnet.getSubnetId())
            .limit(1)  // Single-instance solo necesita 1 subnet
            .toList();

        // ========================================
        // Amazon MQ Broker (RabbitMQ)
        // ========================================
        this.broker = CfnBroker.Builder.create(this, "MqBroker")
            .brokerName(prefix + "-rabbitmq")
            .engineType("RABBITMQ")
            .engineVersion("3.13")
            .hostInstanceType(config.mqInstanceType())
            .deploymentMode("SINGLE_INSTANCE")  // CLUSTER_MULTI_AZ para prod real
            .publiclyAccessible(false)
            .subnetIds(subnetIds)
            .securityGroups(List.of(networkStack.getMqSecurityGroup().getSecurityGroupId()))
            .users(List.of(CfnBroker.UserProperty.builder()
                .username(mqUsername)
                .password(mqSecret.secretValueFromJson("password").unsafeUnwrap())
                .build()))
            .autoMinorVersionUpgrade(true)
            .build();

        // ========================================
        // Outputs
        // ========================================
        CfnOutput.Builder.create(this, "MqBrokerId")
            .exportName(prefix + "-mq-broker-id")
            .value(broker.getRef())
            .description("ID del broker RabbitMQ")
            .build();

        CfnOutput.Builder.create(this, "MqSecretArn")
            .exportName(prefix + "-mq-secret-arn")
            .value(mqSecret.getSecretArn())
            .description("ARN del secreto con credenciales MQ")
            .build();
    }

    // ========================================
    // Getters
    // ========================================

    public CfnBroker getBroker() {
        return broker;
    }

    public Secret getMqSecret() {
        return mqSecret;
    }

    public String getMqUsername() {
        return mqUsername;
    }

    /**
     * Obtiene el endpoint AMQP del broker.
     * Formato: amqps://b-xxxx.mq.us-east-1.amazonaws.com:5671
     */
    public String getAmqpEndpoint() {
        // El endpoint real se obtiene despues del despliegue
        // Por ahora retornamos una referencia
        return broker.getAttrAmqpEndpoints().toString();
    }
}
