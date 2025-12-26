package com.example;

import com.example.config.EnvironmentConfig;
import com.example.stacks.NetworkStack;
import com.example.stacks.DatabaseStack;
import com.example.stacks.MessagingStack;
import com.example.stacks.NotificationStack;
import com.example.stacks.ApplicationStack;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Punto de entrada de la aplicacion CDK.
 *
 * Uso:
 *   cdk deploy --all                    (despliega dev por defecto)
 *   cdk deploy --all -c env=prod        (despliega produccion)
 */
public class TicketeroInfraApp {

    public static void main(final String[] args) {
        App app = new App();

        // Leer ambiente desde contexto (default: dev)
        String envName = (String) app.getNode().tryGetContext("env");
        if (envName == null) {
            envName = "dev";
        }

        // Cargar configuracion segun ambiente
        EnvironmentConfig config = envName.equals("prod")
            ? EnvironmentConfig.prod()
            : EnvironmentConfig.dev();

        System.out.println("===========================================");
        System.out.println("  Desplegando ambiente: " + config.environmentName().toUpperCase());
        System.out.println("===========================================");

        // Configuracion de cuenta y region AWS
        Environment awsEnv = Environment.builder()
            .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
            .region(System.getenv("CDK_DEFAULT_REGION"))
            .build();

        // Prefijo para nombres de stacks
        String prefix = config.resourcePrefix();

        // ========================================
        // STACK 1: Red (VPC, Subnets, etc.)
        // ========================================
        NetworkStack networkStack = new NetworkStack(app, prefix + "-network",
            StackProps.builder()
                .env(awsEnv)
                .description("VPC y redes para Ticketero")
                .build(),
            config);

        // ========================================
        // STACK 2: Base de Datos (RDS PostgreSQL)
        // ========================================
        DatabaseStack databaseStack = new DatabaseStack(app, prefix + "-database",
            StackProps.builder()
                .env(awsEnv)
                .description("RDS PostgreSQL para Ticketero")
                .build(),
            config,
            networkStack);

        // ========================================
        // STACK 3: Mensajeria (Amazon MQ - RabbitMQ)
        // ========================================
        MessagingStack messagingStack = new MessagingStack(app, prefix + "-messaging",
            StackProps.builder()
                .env(awsEnv)
                .description("Amazon MQ RabbitMQ para Ticketero")
                .build(),
            config,
            networkStack);

        // ========================================
        // STACK 4: Notificaciones (Telegram, etc.)
        // ========================================
        NotificationStack notificationStack = new NotificationStack(app, prefix + "-notification",
            StackProps.builder()
                .env(awsEnv)
                .description("Secretos para notificaciones de Ticketero")
                .build(),
            config);

        // ========================================
        // STACK 5: Aplicacion (ECS Fargate + ALB)
        // ========================================
        ApplicationStack applicationStack = new ApplicationStack(app, prefix + "-application",
            StackProps.builder()
                .env(awsEnv)
                .description("ECS Fargate y ALB para Ticketero")
                .build(),
            config,
            networkStack,
            databaseStack,
            messagingStack,
            notificationStack);

        app.synth();
    }
}
